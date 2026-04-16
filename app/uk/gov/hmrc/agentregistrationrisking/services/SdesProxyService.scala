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
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationForRiskingStatusOld
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.connectors.SdesProxyConnector
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRiskingOld
import uk.gov.hmrc.agentregistrationrisking.model.CorrelationIdGenerator
import uk.gov.hmrc.agentregistrationrisking.model.sdes.*
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging
import uk.gov.hmrc.objectstore.client.ObjectSummaryWithMd5
import uk.gov.hmrc.objectstore.client.config.ObjectStoreClientConfig

import java.time.Clock
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
  correlationIdGenerator: CorrelationIdGenerator
)(using
  ExecutionContext,
  Clock
)
extends RequestAwareLogging:

  def notifySdesFileReady(objectSummaryWithMd5: ObjectSummaryWithMd5)(using RequestHeader): Future[Unit] =
    val notifySdesFileReadyRequest: NotifySdesFileReadyRequest = makeNotifySdesFileReadyRequest(objectSummaryWithMd5)
    sdesProxyConnector.notifySdesFileReady(notifySdesFileReadyRequest)

  private def makeNotifySdesFileReadyRequest(objectSummaryWithMd5: ObjectSummaryWithMd5): NotifySdesFileReadyRequest =
    val informationType: SdesInformationType = appConfig.SdesProxy.outboundInformationType
    val serviceReferenceNumber: SdesSrn = appConfig.SdesProxy.srn
    /* SDES do not access our object store directly, instead we provide a location that they can access that proxies to
    ** our object store from outside the platform
     */
    val location = s"${appConfig.SdesProxy.objectStoreLocationPrefix}/${objectSummaryWithMd5.location.directory.asUri}"

    NotifySdesFileReadyRequest(
      informationType = informationType,
      file = NotifySdesFile(
        recipientOrSender = Some(serviceReferenceNumber),
        name = objectSummaryWithMd5.location.fileName,
        location = Some(location),
        checksum = NotifySdesFileReadyChecksum(
          algorithm = SdesChecksumAlgorithm.md5,
          value = objectSummaryWithMd5.contentMd5.value
        ),
        size = objectSummaryWithMd5.contentLength.intValue,
        properties = None
      ),
      audit = NotifySdesAudit(correlationIdGenerator.nextCorrelationId)
    )

  // TODO 1. Decide when to return status. Now is after all processing but that can take time and what if something fail.
  // TODO 2. Now there is no recovery for processResults so if one record fail whole processing will stop and no status will be returned. This is not ideal.  need more discussion on how to handle this.
  def retrieveAndProcessResultsFiles(using request: RequestHeader): Future[Seq[ObjectSummaryWithMd5]] =
    logger.info(s"Results file retrieval started...")
    for
      unprocessedFileList <- resultsFileService.getUnprocessedAvailableFiles()
      _ = logger.info(s"Unprocessed files found: ${unprocessedFileList.size}")
      uploadResults <-
        unprocessedFileList.foldLeft(Future.successful(Seq.empty[ObjectSummaryWithMd5])):
          (
            processedFiles,
            resultsFile
          ) =>
            processedFiles.flatMap: completed =>
              for
                records <- resultsFileService.downloadAndParseRecords(resultsFile)
                _ <- applicationStatusService.processResults(records)
                uploadResult <- resultsFileService.uploadAndLogResultFile(resultsFile)
                updatedApplications <- applicationStatusService.updateApplicationStatuses(records)
                approvedApplications = updatedApplications.filter(_.status === ApplicationForRiskingStatusOld.Approved)
                _ <- subscribeApprovedApplications(approvedApplications)
              yield completed :+ uploadResult
    yield uploadResults

  private def subscribeApprovedApplications(
    approvedApplications: Set[ApplicationForRiskingOld]
  )(using RequestHeader): Future[Unit] = Future.traverse(approvedApplications): application =>
    subscribeAgentService.subscribeAgent(application).flatMap: _ =>
      applicationForRiskingRepo.upsert(application.copy(status = ApplicationForRiskingStatusOld.SubscribedAndEnrolled))
    .recover:
      case ex: Throwable => logger.error(s"Failed to subscribe application ${application.applicationReference.value}: ${ex.getMessage}")
  .map(_ => ())
