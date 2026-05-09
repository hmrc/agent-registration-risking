/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentregistrationrisking.services

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcome
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcome.*
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector.EnrolmentRequest
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector.KnownFact
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector.KnownFactsRequest
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.agentregistrationrisking.connectors.HipConnector
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.RiskingResult
import uk.gov.hmrc.agentregistrationrisking.model.hip.Arn
import uk.gov.hmrc.agentregistrationrisking.model.hip.SubscribeAgentRequest
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.util.ProcessInSequence
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.chaining.scalaUtilChainingOps

@Singleton
class SubscriptionService @Inject() (
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  individualForRiskingRepo: IndividualForRiskingRepo,
  hipConnector: HipConnector,
  enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector,
  clock: Clock
)(using ExecutionContext)
extends RequestAwareLogging:

  def subscribeApprovedApplications()(using RequestHeader): Future[Unit] =
    logger.info("Subscribing approved applications...")
    for
      applications: Seq[ApplicationForRisking] <- findApprovedReadyToSubscribe()
      applicationCount: Int = applications.size
      _ = logger.info(s"Found $applicationCount applications ready to subscribe")
      subscriptionSuccessCount <- ProcessInSequence
        .processInSequence(applications): application =>
          subscribeApplication(application)
            .map(_ => true)
            .recover:
              case ex: Throwable =>
                logger.error(s"Failed to subscribe agent: ${application.agentApplication.applicationReference.value}: ${ex.getMessage}")
                false
        .map(_.count(identity))
      _ = logger.info(s"Subscribed $subscriptionSuccessCount/$applicationCount applications")
    yield ()

  private def subscribeApplication(application: ApplicationForRisking)(using RequestHeader): Future[Unit] =
    logger.info(s"Subscribing application: ${application.applicationReference} ...")
    for
      _ <- subscribeAgent(application.agentApplication)
      _ <- applicationForRiskingRepo.upsert(application.copy(isSubscribed = true, lastUpdatedAt = Instant.now(clock)))
      _ = logger.info(s"Application subscribed: ${application.applicationReference}")
    yield ()

  private def findApprovedReadyToSubscribe()(using RequestHeader): Future[Seq[ApplicationForRisking]] =
    for
      applications: Seq[ApplicationForRisking] <- applicationForRiskingRepo.findNotSubscribedWithResults()
      individuals: Seq[IndividualForRisking] <- individualForRiskingRepo.findByApplicationReferences(applications.map(_.applicationReference))
      applicationsWithIndividuals: Seq[ApplicationWithIndividuals] = ApplicationWithIndividuals.merge(applications, individuals)
      approvedApplicationsWithIndividuals: Seq[ApplicationWithIndividuals] = applicationsWithIndividuals
        .filter: applicationWithIndividuals =>
          RiskingOutcomeHelper
            .computeRiskingOutcome(applicationWithIndividuals)
            .exists(_ === RiskingOutcome.Approved)
      approvedApplications: Seq[ApplicationForRisking] = approvedApplicationsWithIndividuals.map(_.application)
    yield approvedApplications

  private def subscribeAgent(agentApplication: AgentApplication)(using RequestHeader): Future[Unit] =
    val agentDetails = agentApplication.getAgentDetails
    val amlsDetails = agentApplication.getAmlsDetails
    val subscribeAgentRequest: SubscribeAgentRequest = SubscribeAgentRequest(
      name = agentDetails.businessName.getAgentBusinessName,
      addr1 = agentDetails.getAgentCorrespondenceAddress.addressLine1,
      addr2 = agentDetails.getAgentCorrespondenceAddress.addressLine2.getOrElse(""),
      addr3 = agentDetails.getAgentCorrespondenceAddress.addressLine3,
      addr4 = agentDetails.getAgentCorrespondenceAddress.addressLine4,
      postcode = agentDetails.getAgentCorrespondenceAddress.postalCode,
      country = agentDetails.getAgentCorrespondenceAddress.countryCode,
      phone = Some(agentDetails.getTelephoneNumber.agentTelephoneNumber),
      email = agentDetails.getAgentEmailAddress.getEmailAddress,
      supervisoryBody = Some(amlsDetails.supervisoryBody.value),
      membershipNumber = Some(amlsDetails.getRegistrationNumber.value),
      evidenceObjectReference = None,
      updateDetailsStatus = "ACCEPTED",
      amlSupervisionUpdateStatus = "ACCEPTED",
      directorPartnerUpdateStatus = "ACCEPTED",
      acceptNewTermsStatus = "ACCEPTED",
      reriskStatus = "ACCEPTED"
    )

    val knownFacts: Seq[KnownFact] = Seq(
      KnownFact(
        key = "AgencyPostcode",
        value = subscribeAgentRequest.postcode.getOrThrowExpectedDataMissing("postcode is required for UK subscriptions")
      )
    )

    for
      arn <- hipConnector.subscribeToAgentServices(
        safeId = agentApplication.getSafeId,
        subscribeAgentRequest = subscribeAgentRequest
      )
      _ = logger.info(s"Subscribed to agent services: ${agentApplication.applicationReference}")
      enrolmentKey = s"HMRC-AS-AGENT~AgentReferenceNumber~${arn.value}"
      _ <- enrolmentStoreProxyConnector.addKnownFacts(
        enrolmentKey = enrolmentKey,
        knownFactsRequest = KnownFactsRequest(verifiers = knownFacts)
      )
      _ = logger.info(s"Added known fact: ${agentApplication.applicationReference}")
      _ <- enrolmentStoreProxyConnector.allocateEnrolmentToGroup(
        enrolmentKey = enrolmentKey,
        groupId = agentApplication.groupId,
        enrolmentRequest = EnrolmentRequest(
          userId = agentApplication.applicantCredentials.providerId,
          `type` = "principal",
          friendlyName = subscribeAgentRequest.name,
          verifiers = knownFacts
        )
      )
      _ = logger.info(s"Allocated enrolment to group: ${agentApplication.applicationReference}")
    yield ()
