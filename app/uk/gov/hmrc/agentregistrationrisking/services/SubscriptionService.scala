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
import uk.gov.hmrc.agentregistration.shared.Arn
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicationData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.AgentDetailsData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.AmlsDetailsData
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistrationrisking.audit.AuditService
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector.EnrolmentRequest
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector.KnownFact
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector.KnownFactsRequest
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.agentregistrationrisking.connectors.HipConnector
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.hip.SubscribeAgentRequest
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.util.ProcessInSequence
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class SubscriptionService @Inject() (
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  hipConnector: HipConnector,
  enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector,
  clock: Clock,
  auditService: AuditService
)(using ExecutionContext)
extends RequestAwareLogging:

  def processSubscriptions()(using RequestHeader): Future[Unit] =
    logger.info("Subscribing approved applications...")
    for
      applications: Seq[ApplicationForRisking] <- applicationForRiskingRepo.findReadyToBeSubscribed()
      applicationCount: Int = applications.size
      _ = logger.info(s"Found $applicationCount applications ready to subscribe")
      subscriptionSuccessCount <-
        ProcessInSequence.processAllInSequence(applications)(subscribeApplication):
          case (ex, application) => logger.error(s"Failed to subscribe agent: ${application.applicationData.applicationReference.value}", ex)
      _ = logger.info(s"Subscribed $subscriptionSuccessCount/$applicationCount applications")
    yield ()

  private def subscribeApplication(application: ApplicationForRisking)(using RequestHeader): Future[Unit] =
    logger.info(s"Subscribing application: ${application.applicationReference} ...")
    for
      _ <- subscribeAgent(application.applicationData)
      _ <- applicationForRiskingRepo.upsert(application.copy(isSubscribed = true, lastUpdatedAt = Instant.now(clock)))
      _ = logger.info(s"Application subscribed: ${application.applicationReference}")
    yield ()

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  private def subscribeAgent(agentApplication: ApplicationData)(using RequestHeader): Future[Unit] =
    val agentDetails: AgentDetailsData = agentApplication.agentDetails
    val amlsDetails: AmlsDetailsData = agentApplication.amlsDetails
    val subscribeAgentRequest: SubscribeAgentRequest = SubscribeAgentRequest(
      name = ensureFieldLength(agentDetails.businessName.getAgentBusinessName, 40).asInstanceOf[String],
      addr1 = ensureFieldLength(agentDetails.agentCorrespondenceAddress.addressLine1, 35).asInstanceOf[String],
      addr2 = ensureFieldLength(agentDetails.agentCorrespondenceAddress.addressLine2.getOrElse(""), 35).asInstanceOf[String],
      addr3 = ensureFieldLength(agentDetails.agentCorrespondenceAddress.addressLine3, 35).asInstanceOf[Option[String]],
      addr4 = ensureFieldLength(agentDetails.agentCorrespondenceAddress.addressLine4, 35).asInstanceOf[Option[String]],
      postcode = ensureFieldLength(agentDetails.agentCorrespondenceAddress.postalCode, 10).asInstanceOf[Option[String]],
      country = ensureCountryCode(agentDetails.agentCorrespondenceAddress.countryCode),
      phone = ensureFieldLength(Some(agentDetails.telephoneNumber.getAgentTelephoneNumber), 24).asInstanceOf[Option[String]],
      email = ensureFieldLength(agentDetails.agentEmailAddress, 132).asInstanceOf[EmailAddress],
      supervisoryBody = Some(amlsDetails.supervisoryBody.value),
      membershipNumber = Some(amlsDetails.amlsRegistrationNumber.value),
      evidenceObjectReference = None,
      updateDetailsStatus = "ACCEPTED",
      amlSupervisionUpdateStatus = "ACCEPTED",
      directorPartnerUpdateStatus = "ACCEPTED",
      acceptNewTermsStatus = "ACCEPTED",
      reriskStatus = "ACCEPTED"
    )

    for
      arn <-
        if (agentApplication.arn.nonEmpty)
          logger.info(s"Agent is already subscribed to agent services (skipping hip request): ${agentApplication.applicationReference}")
          Future.successful(agentApplication.getArn)
        else
          hipConnector.subscribeToAgentServices(
            safeId = agentApplication.safeId,
            subscribeAgentRequest = subscribeAgentRequest
          ).map(arn =>
            logger.info(s"Subscribed to agent services: ${agentApplication.applicationReference}")
            arn
          )
      _ <- enrolAgent(
        arn,
        agentApplication,
        subscribeAgentRequest
      )
      _ = auditService.sendCreateAgentServicesAccountEvent(agentApplication, arn)
      _ = logger.info("Sent CreatedAgentServicesAccountAuditEvent")
    yield ()

  private def enrolAgent(
    arn: Arn,
    agentApplication: ApplicationData,
    subscribeAgentRequest: SubscribeAgentRequest
  )(using RequestHeader): Future[Unit] =
    val knownFacts: Seq[KnownFact] = Seq(
      KnownFact(
        key = "AgencyPostcode",
        value = subscribeAgentRequest.postcode.getOrThrowExpectedDataMissing("postcode is required for UK subscriptions")
      )
    )
    val enrolmentKey = s"HMRC-AS-AGENT~AgentReferenceNumber~${arn.value}"
    for
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

  private def ensureCountryCode(country: String)(using RequestHeader): String =
    val gbCountries: Set[String] = Set(
      "GB",
      "GREAT BRITAIN",
      "BRITAIN",
      "UNITED KINGDOM",
      "UK",
      "ENGLAND",
      "SCOTLAND",
      "WALES",
      "NORTHERN IRELAND"
    )
    if gbCountries.contains(country.toUpperCase)
    then "GB"
    else
      logger.info(s"Non-UK country provided: $country. Attempting to use first two characters as country code.")
      country.take(2).toUpperCase // this may or may not be a valid country code and may still be rejected by the API, but we have no better option for non-UK countries and the API will return a clear error if the code is invalid as opposed to a country code too long error

  private def ensureFieldLength(
    field: String | Option[String] | EmailAddress,
    max: Int
  ): String | Option[String] | EmailAddress =
    field match {
      case str: String => str.take(max)
      case opt: Option[String] => opt.map(_.take(max))
      case email: EmailAddress => EmailAddress(email.value.take(max))
    }
