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

import com.softwaremill.quicklens.modify
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.Arn
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicationData
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistrationrisking.audit.AuditService
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector.EnrolmentRequest
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector.KnownFact
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector.KnownFactsRequest
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.agentregistrationrisking.connectors.HipConnector
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.EnrolmentFailure
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
        ProcessInSequence.processAllInSequence(applications)(createAgentServicesAccount):
          case (ex, application) => logger.error(s"Failed to subscribe agent: ${application.applicationData.applicationReference.value}", ex)
      _ = logger.info(s"Subscribed $subscriptionSuccessCount/$applicationCount applications")
    yield ()

  private def createAgentServicesAccount(application: ApplicationForRisking)(using RequestHeader): Future[Unit] =
    logger.info(s"Creating agent services account: ${application.applicationReference} ...")
    val applicationData: ApplicationData = application.applicationData
    val subscribeAgentRequest: SubscribeAgentRequest = SubscribeAgentRequestBuilder.buildSubscribeAgentRequest(applicationData)
    for
      arn: Arn <- subscribeToAgentServices(applicationData, subscribeAgentRequest)
      enrolmentFailure: Option[EnrolmentFailure] <- enrolToAgentServices(
        arn,
        applicationData,
        subscribeAgentRequest
      )
      _ <-
        enrolmentFailure match
          case None =>
            auditService.sendCreateAgentServicesAccountEvent(applicationData, arn)
            markAccountCreated(application, arn)
          case Some(enrolmentFailure) =>
            stopRetryingAccountCreation(
              application,
              arn,
              enrolmentFailure
            )
    yield ()

  private def subscribeToAgentServices(
    applicationData: ApplicationData,
    subscribeAgentRequest: SubscribeAgentRequest
  )(using RequestHeader): Future[Arn] =
    if applicationData.arn.nonEmpty then
      logger.info(s"Agent is already subscribed to agent services (skipping hip request): ${applicationData.applicationReference}")
      Future.successful(applicationData.getArn)
    else
      hipConnector.subscribeToAgentServices(
        safeId = applicationData.safeId,
        subscribeAgentRequest = subscribeAgentRequest
      ).map: arn =>
        logger.info(s"Subscribed to agent services: ${applicationData.applicationReference}")
        arn

  private def enrolToAgentServices(
    arn: Arn,
    applicationData: ApplicationData,
    subscribeAgentRequest: SubscribeAgentRequest
  )(using RequestHeader): Future[Option[EnrolmentFailure]] =
    val knownFacts: Seq[KnownFact] = Seq(
      KnownFact(
        key = "AgencyPostcode",
        value = subscribeAgentRequest.postcode.getOrThrowExpectedDataMissing("postcode is required for UK subscriptions")
      )
    )
    val enrolmentKey: String = s"HMRC-AS-AGENT~AgentReferenceNumber~${arn.value}"
    for
      _ <- enrolmentStoreProxyConnector.addKnownFacts(
        enrolmentKey = enrolmentKey,
        knownFactsRequest = KnownFactsRequest(verifiers = knownFacts)
      )
      _ = logger.info(s"Added known fact: ${applicationData.applicationReference}")
      enrolmentFailure: Option[EnrolmentFailure] <- enrolmentStoreProxyConnector.allocateEnrolmentToGroup(
        enrolmentKey = enrolmentKey,
        groupId = applicationData.groupId,
        enrolmentRequest = EnrolmentRequest(
          userId = applicationData.applicantCredentials.providerId,
          `type` = "principal",
          friendlyName = subscribeAgentRequest.name,
          verifiers = knownFacts
        )
      )
    yield enrolmentFailure

  private def markAccountCreated(
    application: ApplicationForRisking,
    arn: Arn
  )(using RequestHeader): Future[Unit] = applicationForRiskingRepo.upsert(
    application
      .modify(_.applicationData.arn).setTo(Some(arn))
      .modify(_.isSubscribed).setTo(true)
      .modify(_.lastUpdatedAt).setTo(Instant.now(clock))
  ).map: _ =>
    logger.info(s"Agent services account created: ${application.applicationReference}")

  private def stopRetryingAccountCreation(
    application: ApplicationForRisking,
    arn: Arn,
    enrolmentFailure: EnrolmentFailure
  )(using RequestHeader): Future[Unit] = applicationForRiskingRepo.upsert(
    application
      .modify(_.applicationData.arn).setTo(Some(arn))
      .modify(_.enrolmentFailure).setTo(Some(enrolmentFailure))
      .modify(_.lastUpdatedAt).setTo(Instant.now(clock))
  ).map: _ =>
    logger.warn(s"Stopped retrying agent services account creation for ${application.applicationReference}: non-recoverable $enrolmentFailure")
