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
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.connectors.SdesProxyConnector
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.model.CorrelationIdGenerator
import uk.gov.hmrc.agentregistrationrisking.model.sdes.*
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging
import uk.gov.hmrc.objectstore.client.Md5Hash
import uk.gov.hmrc.objectstore.client.ObjectSummaryWithMd5
import uk.gov.hmrc.objectstore.client.config.ObjectStoreClientConfig
import uk.gov.hmrc.agentregistrationrisking.util.Utils.*

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class SdesProxyService @Inject() (
  sdesProxyConnector: SdesProxyConnector,
  resultsFileService: ResultsFileService,
  applicationStatusService: ApplicationStatusService,
  subscribeAgentService: SubscribeAgentService,
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  appConfig: AppConfig,
  objectStoreClientConfig: ObjectStoreClientConfig,
  objectStoreService: ObjectStoreService,
  correlationIdGenerator: CorrelationIdGenerator
)(using
  ExecutionContext,
  Clock
)
extends RequestAwareLogging:

  def notifySdesFileReady(objectSummaryWithMd5: ObjectSummaryWithMd5)(using RequestHeader): Future[Unit] =
    val fileReadyNotification = makeNotifySdesFileReadyRequest(objectSummaryWithMd5)
    sdesProxyConnector.notifySdesFileReady(fileReadyNotification)

  private def makeNotifySdesFileReadyRequest(objectSummaryWithMd5: ObjectSummaryWithMd5)(using RequestHeader): NotifySdesFileReadyRequest = {
    val informationType: SdesInformationType = appConfig.SdesProxy.outboundInformationType
    val serviceReferenceNumber: SdesSrn = appConfig.SdesProxy.srn
    val objectStoreLocation = s"${appConfig.SdesProxy.objectStoreLocationPrefix}/${objectSummaryWithMd5.location.directory.asUri}"
    val fileName = s"${objectSummaryWithMd5.location.fileName}"

    NotifySdesFileReadyRequest(
      informationType = informationType,
      file = NotifySdesFile(
        recipientOrSender = Some(serviceReferenceNumber),
        name = objectSummaryWithMd5.location.fileName,
        location = Some(s"$objectStoreLocation$fileName"),
        checksum = NotifySdesFileReadyChecksum(
          algorithm = SdesChecksumAlgorithm.md5,
          value = base64ToHex(objectSummaryWithMd5.contentMd5.value)
        ),
        size = objectSummaryWithMd5.contentLength.intValue,
        properties = None
      ),
      audit = NotifySdesAudit(correlationIdGenerator.nextCorrelationId)
    )
  }

  def retrieveAndProcessResultsFiles(using request: RequestHeader): Future[Seq[ObjectSummaryWithMd5]] =
    logger.info(s"Results file retrieval started...")
    for
      unprocessedAvailableFiles: Seq[AvailableFile] <- resultsFileService.getUnprocessedAvailableFiles()
      _ = logger.info(s"Found ${unprocessedAvailableFiles.size} unprocessed available results file(s) to process")
      uploadResults <-
        unprocessedAvailableFiles.foldLeft(Future.successful(Seq.empty[ObjectSummaryWithMd5])):
          (
            processedFiles: Future[Seq[ObjectSummaryWithMd5]],
            availableFile: AvailableFile
          ) =>
            processedFiles.flatMap: (completed: Seq[ObjectSummaryWithMd5]) =>
              for
                riskingResultRecords <- resultsFileService.downloadAndParseRecords(availableFile)
                _ <- applicationStatusService.processResults(riskingResultRecords)
                uploadResult: ObjectSummaryWithMd5 <- resultsFileService.uploadAndLogResultFile(availableFile)
                applicationsWithIndividuals: Seq[ApplicationWithIndividuals] <-
                  applicationStatusService.getAllUnsubscribedApplicationsWithIndividualsWithResults
                approvedApplicationsWithIndividuals = applicationStatusService.getApprovedApplicationsWithIndividuals(applicationsWithIndividuals)
                _ <- subscribeApprovedApplications(approvedApplicationsWithIndividuals)
              yield completed :+ uploadResult
    yield uploadResults

  private def subscribeApprovedApplications(
    approvedApplications: Seq[ApplicationWithIndividuals]
  )(using RequestHeader): Future[Unit] = Future.traverse(approvedApplications): appWithIndividuals =>
    subscribeAgentService.subscribeAgent(appWithIndividuals.application).flatMap: _ =>
      applicationForRiskingRepo.upsert(appWithIndividuals.application.copy(isSubscribed = true, lastUpdatedAt = Instant.now(summon[Clock])))
    .recover:
      case ex: Throwable =>
        logger.error(s"Failed to subscribe application ${appWithIndividuals.application.agentApplication.applicationReference.value}: ${ex.getMessage}")
  .map(_ => ())
