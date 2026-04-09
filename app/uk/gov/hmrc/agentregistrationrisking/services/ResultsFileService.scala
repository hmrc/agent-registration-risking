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
import uk.gov.hmrc.agentregistrationrisking.connectors.RiskingResultsConnector
import uk.gov.hmrc.agentregistrationrisking.connectors.SdesProxyConnector
import uk.gov.hmrc.agentregistrationrisking.model.RiskingResultRecord
import uk.gov.hmrc.agentregistrationrisking.model.sdes.AvailableFile
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging
import uk.gov.hmrc.objectstore.client.ObjectListing
import uk.gov.hmrc.objectstore.client.ObjectSummaryWithMd5

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ResultsFileService @Inject() (
  sdesProxyConnector: SdesProxyConnector,
  objectStoreService: ObjectStoreService,
  riskingResultsConnector: RiskingResultsConnector
)(using ExecutionContext)
extends RequestAwareLogging:

  def downloadAndParseRecords(file: AvailableFile)(using request: RequestHeader): Future[List[RiskingResultRecord]] =
    logger.info(s"Attempting to downloading ${file.filename}")
    riskingResultsConnector.getRiskingFile(availableFile = file)

  def uploadAndLogResultFile(file: AvailableFile)(using request: RequestHeader): Future[ObjectSummaryWithMd5] =
    val downloadUri = Uri(file.downloadURL).toJavaUri.toURL
    val fileName = file.filename
    for
      uploadResult <- objectStoreService.uploadFromUrl(downloadUrl = downloadUri, fileName = fileName)
      _ = logger.info(s"Uploaded file to object store: $fileName")
    yield uploadResult

  def getUnprocessedAvailableFiles()(using request: RequestHeader): Future[Seq[AvailableFile]] =
    for
      availableFiles: Seq[AvailableFile] <- sdesProxyConnector.listAvailableFiles
      filesAlreadyProcessed: ObjectListing <- objectStoreService.listObjects
      _ = logger.info(s"Files already processed: ${filesAlreadyProcessed.objectSummaries.size}")
      fileNamesAlreadyProcessed = filesAlreadyProcessed.objectSummaries.map(_.location.fileName).toSet
      _ = logger.info(s"File Names already processed: ${fileNamesAlreadyProcessed.toString}")
      unprocessedAvailableFiles = availableFiles.filterNot(f => fileNamesAlreadyProcessed.contains(f.filename))
    yield unprocessedAvailableFiles
