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
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationrisking.audit.AuditService
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector.AssignEnrolmentResult
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector.AssignedEnrolment
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector.QueryEnrolmentsResult
import uk.gov.hmrc.agentregistrationrisking.connectors.HipConnector
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.EnrolmentFailure
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
class AlreadyAllocatedToGroupRecoveryService @Inject() (
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector,
  hipConnector: HipConnector,
  clock: Clock,
  auditService: AuditService,
  appConfig: AppConfig
)(using ExecutionContext)
extends RequestAwareLogging:

  def processRecoveries()(using RequestHeader): Future[Unit] =
    logger.info("Recovering AlreadyAllocatedToGroup applications: Started...")
    for
      applications: Seq[ApplicationForRisking] <- applicationForRiskingRepo.findReadyForAlreadyAllocatedToGroupRecovery()
      applicationCount: Int = applications.size
      _ = logger.info(s"Recovering AlreadyAllocatedToGroup applications: found $applicationCount parked applications")
      processedCount <-
        ProcessInSequence.processAllInSequence(applications)(recoverApplication):
          case (ex, application) => logger.error(s"Recovering AlreadyAllocatedToGroup applications: FAILED for ${application.applicationReference.value}", ex)
      _ = logger.info(s"Recovering AlreadyAllocatedToGroup applications: finished $processedCount of $applicationCount attempts DONE")
    yield ()

  private def recoverApplication(application: ApplicationForRisking)(using RequestHeader): Future[Unit] =
    application.applicationData.arn match
      case None => recoverApplicationWithoutArn(application)
      case Some(arn) => recoverApplicationWithArn(application, arn)

  private def recoverApplicationWithoutArn(application: ApplicationForRisking)(using RequestHeader): Future[Unit] =
    val applicationData: ApplicationData = application.applicationData
    logger.info(s"Recovering AlreadyAllocatedToGroup applications: no ARN recorded, subscribing to agent services to obtain it for ${application.applicationReference.value}")
    for
      arn: Arn <- hipConnector.subscribeToAgentServices(
        safeId = applicationData.safeId,
        subscribeAgentRequest = SubscribeAgentRequestBuilder.buildSubscribeAgentRequest(applicationData)
      )
      applicationWithArn: ApplicationForRisking = application.modify(_.applicationData.arn).setTo(Some(arn))
      _ <- recoverApplicationWithArn(applicationWithArn, arn)
    yield ()

  private def recoverApplicationWithArn(
    application: ApplicationForRisking,
    arn: Arn
  )(using RequestHeader): Future[Unit] =
    val providerId: String = application.applicationData.applicantCredentials.providerId
    val serviceName: String = appConfig.hmrcAsAgentEnrolment.key
    logger.info(s"Recovering AlreadyAllocatedToGroup applications: checking the applicant credential for ${application.applicationReference.value}")
    for
      queryEnrolmentsResult: QueryEnrolmentsResult <- enrolmentStoreProxyConnector.queryPrincipalEnrolmentsForUser(userId = providerId, service = serviceName)
      _ <-
        queryEnrolmentsResult match
          case QueryEnrolmentsResult.CredentialNotFound => stopRetryingRecovery(application, EnrolmentFailure.CredentialDoesNotExist)
          case QueryEnrolmentsResult.QueryRejected => stopRetryingRecovery(application, EnrolmentFailure.EnrolmentQueryRejected)
          case QueryEnrolmentsResult.Enrolments(assignedEnrolments) =>
            recoverFromAssignedEnrolments(
              application,
              arn,
              arnsOnCredential(assignedEnrolments)
            )
    yield ()

  // enrolment state is not checked: ES11 cannot activate, so a NotYetActivated enrolment on the credential still counts as held
  private def arnsOnCredential(assignedEnrolments: Seq[AssignedEnrolment]): Seq[Arn] = assignedEnrolments
    .flatMap(_.identifiers)
    .filter(_.key === "AgentReferenceNumber")
    .map(enrolmentIdentifier => Arn(enrolmentIdentifier.value))

  private def recoverFromAssignedEnrolments(
    application: ApplicationForRisking,
    arn: Arn,
    arnsOnCredential: Seq[Arn]
  )(using RequestHeader): Future[Unit] =
    arnsOnCredential match
      case Seq() => assignEnrolmentToApplicantCredential(application, arn)
      case arns if arns.contains(arn) =>
        logger.info(s"Recovering AlreadyAllocatedToGroup applications: credential already holds ${enrolmentKeyFor(arn)} for ${application.applicationReference.value}")
        markAccountCreated(application, arn)
      case arns =>
        logger.warn(s"Recovering AlreadyAllocatedToGroup applications: credential holds ${appConfig.hmrcAsAgentEnrolment.key} with [${arns.map(_.value).mkString(", ")}] not ${arn.value} for ${application.applicationReference.value}")
        stopRetryingRecovery(application, EnrolmentFailure.GroupEnrolledForDifferentArn)

  private def assignEnrolmentToApplicantCredential(
    application: ApplicationForRisking,
    arn: Arn
  )(using RequestHeader): Future[Unit] =
    val providerId: String = application.applicationData.applicantCredentials.providerId
    val enrolmentKey: String = enrolmentKeyFor(arn)
    logger.info(s"Recovering AlreadyAllocatedToGroup applications: assigning $enrolmentKey to the credential for ${application.applicationReference.value}")
    enrolmentStoreProxyConnector
      .assignEnrolmentToUser(userId = providerId, enrolmentKey = enrolmentKey)
      .flatMap:
        case AssignEnrolmentResult.Assigned => markAccountCreated(application, arn)
        case AssignEnrolmentResult.NotAllocatedToGroup => stopRetryingRecovery(application, EnrolmentFailure.GroupEnrolledForDifferentArn)
        case AssignEnrolmentResult.UserDoesNotExist => stopRetryingRecovery(application, EnrolmentFailure.CredentialDoesNotExist)
        case AssignEnrolmentResult.AssignmentRejected => stopRetryingRecovery(application, EnrolmentFailure.EnrolmentAssignmentRejected)
        case AssignEnrolmentResult.AllocationDoesNotExist => stopRetryingRecovery(application, EnrolmentFailure.EnrolmentNotAllocated)

  private def enrolmentKeyFor(arn: Arn): String = s"${appConfig.hmrcAsAgentEnrolment.key}~AgentReferenceNumber~${arn.value}"

  private def markAccountCreated(
    application: ApplicationForRisking,
    arn: Arn
  )(using RequestHeader): Future[Unit] =
    auditService.sendCreateAgentServicesAccountEvent(application.applicationData, arn)
    applicationForRiskingRepo.upsert(
      application
        .modify(_.isSubscribed).setTo(true)
        .modify(_.enrolmentFailure).setTo(None)
        .modify(_.lastUpdatedAt).setTo(Instant.now(clock))
    ).map: _ =>
      logger.info(s"Recovering AlreadyAllocatedToGroup applications: recovered ${application.applicationReference.value}")

  private def stopRetryingRecovery(
    application: ApplicationForRisking,
    enrolmentFailure: EnrolmentFailure
  )(using RequestHeader): Future[Unit] = applicationForRiskingRepo.upsert(
    application
      .modify(_.enrolmentFailure).setTo(Some(enrolmentFailure))
      .modify(_.lastUpdatedAt).setTo(Instant.now(clock))
  ).map: _ =>
    logger.warn(s"Recovering AlreadyAllocatedToGroup applications: parked ${application.applicationReference.value} as $enrolmentFailure")
