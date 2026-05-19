/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileWithContent
import uk.gov.hmrc.agentregistrationrisking.model.RiskingResultRecords
import uk.gov.hmrc.agentregistrationrisking.util.ProcessInSequence
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationrisking.util.RequestSupport.hc
import uk.gov.hmrc.objectstore.client.ObjectListing
import uk.gov.hmrc.objectstore.client.ObjectSummaryWithMd5
import uk.gov.hmrc.objectstore.client.Path
import uk.gov.hmrc.objectstore.client.PresignedDownloadUrl
import uk.gov.hmrc.objectstore.client.RetentionPeriod
import uk.gov.hmrc.objectstore.client.play.Implicits.*
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient

import java.net.URL
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ObjectStoreService @Inject() (
  playObjectStoreClient: PlayObjectStoreClient,
  appConfig: AppConfig
)(using
  ExecutionContext,
  Clock
)
extends RequestAwareLogging:

  private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
  private val receivedResultsFilesPath = Path.Directory("processed-results-files")

  def deleteSdesFiles()(using request: RequestHeader) =
    for
      objectListing: ObjectListing <- playObjectStoreClient.listObjects(Path.Directory("sdes"))
      _ = logger.info(s"Deleting objects from object store:...\n ${objectListing.objectSummaries.mkString("\n")}")
      _ <-
        ProcessInSequence.processAllInSequence(objectListing.objectSummaries)(objectSummary =>
          playObjectStoreClient
            .deleteObject(objectSummary.location)
            .map(_ => logger.info(s"Deleted ${objectSummary}"))
        ):
          case (throwable, item) => logger.error(s"Failed to delete $item", throwable)
    yield ()

  def uploadRiskingFile(riskingFileWithContent: RiskingFileWithContent)(using request: RequestHeader): Future[ObjectSummaryWithMd5] =
    playObjectStoreClient.putObject[RiskingFileWithContent.RiskingFileContent](
      path = Path.Directory("sdes").file(fileName = riskingFileWithContent.riskingFile.riskingFileName.value),
      content = riskingFileWithContent.riskingFileContent,
      retentionPeriod = RetentionPeriod.SixMonths, // TODO: how long do we need to keep these files?
      contentType = Some("plain/text"),
      contentMd5 = None // defaults to None, and will be calculated
      // owner  =  // defaults to 'appName' configuration
    )

  def uploadRiskingResultsFile(
    riskingResultRecords: RiskingResultRecords
  )(using request: RequestHeader): Future[ObjectSummaryWithMd5] = playObjectStoreClient.putObject(
    path = receivedResultsFilesPath.file(fileName = riskingResultRecords.fileName),
    content = riskingResultRecords.rawContent,
    retentionPeriod = RetentionPeriod.SixMonths, // TODO: how long do we need to keep these files?
    contentType = Some("plain/text"),
    contentMd5 = None // defaults to None, and will be calculated
    // owner  =  // defaults to 'appName' configuration
  )

  def listObjects(using request: RequestHeader): Future[ObjectListing] = playObjectStoreClient.listObjects(receivedResultsFilesPath)

  def generatePreSignedDownloadUrl(
    objectStorePath: Path.Directory,
    objectStoreFileName: String
  )(using request: RequestHeader): Future[PresignedDownloadUrl] = playObjectStoreClient.presignedDownloadUrl(
    path = Path.Directory("sdes").file(fileName = objectStoreFileName)
  )
