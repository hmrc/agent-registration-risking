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
import sttp.model.Uri
import uk.gov.hmrc.agentregistrationrisking.connectors.SdesProxyConnector
import uk.gov.hmrc.agentregistrationrisking.model.RiskingResultsFile
import uk.gov.hmrc.agentregistrationrisking.model.ResultsFileName
import uk.gov.hmrc.agentregistrationrisking.model.ResultsFileProcessingStatus.Downloaded
import uk.gov.hmrc.agentregistrationrisking.model.sdes.AvailableFile
import uk.gov.hmrc.agentregistrationrisking.repository.ResultsFileLogRepo
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging
import uk.gov.hmrc.objectstore.client.ObjectSummaryWithMd5

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ResultsFileService @Inject() (
  sdesProxyConnector: SdesProxyConnector,
  objectStoreService: ObjectStoreService,
  resultsFileLogRepo: ResultsFileLogRepo
)(using
  ExecutionContext,
  Clock
)
extends RequestAwareLogging:

  def retrieveAndProcessResultsFiles(using request: RequestHeader): Future[Seq[ObjectSummaryWithMd5]] =
    logger.info(s"Results file retrieval started...")
    for
      unprocessedFileList <- getUnprocessedAvailableFiles
      _ = logger.info(s"Unprocessed files found: ${unprocessedFileList.size}")
      uploadUnprocessedFiles = unprocessedFileList.map(uploadAndLogResultFile)
      uploadResults <- Future.sequence(uploadUnprocessedFiles)
    yield uploadResults

  private def uploadAndLogResultFile(file: AvailableFile)(using request: RequestHeader): Future[ObjectSummaryWithMd5] =
    val downloadUri = Uri(file.downloadURL).toJavaUri.toURL
    val fileName = file.filename
    for
      uploadResult <- objectStoreService.uploadFromUrl(downloadUrl = downloadUri, fileName = fileName)
      _ = logger.info(s"Uploaded file to object store: $fileName")
      _ <- upsertDownloadedFile(fileName).recoverWith: upsertError =>
        logger.warn(s"Mongo upsert failed for $fileName", upsertError)
        Future.failed(upsertError)
      _ = logger.info(s"File details stored in mongo: $fileName")
    yield uploadResult

  private def upsertDownloadedFile(fileName: String)(using request: RequestHeader): Future[Unit] = resultsFileLogRepo.upsert(RiskingResultsFile(
    fileName = ResultsFileName(fileName),
    status = Downloaded,
    downloadedAt = Instant.now(summon[Clock])
  ))

  private def getUnprocessedAvailableFiles(using request: RequestHeader): Future[Seq[AvailableFile]] =
    for
      availableFiles <- sdesProxyConnector.listAvailableFiles
      filesAlreadyProcessed <- resultsFileLogRepo.findAll()
      fileNamesAlreadyProcessed = filesAlreadyProcessed.map(_.fileName.value).toSet
      unprocessedAvailableFiles = availableFiles.filterNot(f => fileNamesAlreadyProcessed.contains(f.filename))
    yield unprocessedAvailableFiles
