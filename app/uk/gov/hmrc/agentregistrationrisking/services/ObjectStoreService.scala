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
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationrisking.util.RequestSupport.hc
import uk.gov.hmrc.objectstore.client.*
import uk.gov.hmrc.objectstore.client.play.Implicits.*
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient

import java.net.URL
import java.time.Clock
import java.time.LocalDateTime
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

  def put(fileContent: String)(using request: RequestHeader): Future[ObjectSummaryWithMd5] =

    val timestamp: String = LocalDateTime
      .now(summon[Clock])
      .format(formatter)
    val fileName = s"asa_risking_file_version1_0_4_$timestamp.txt"

    playObjectStoreClient.putObject[String](
      path = Path.Directory("applications-for-risking").file(fileName = fileName),
      content = fileContent,
      retentionPeriod = RetentionPeriod.SixMonths, // TODO: how long do we need to keep these files?
      contentType = Some("plain/text"),
      contentMd5 = None // defaults to None, and will be calculated
      // owner  =  // defaults to 'appName' configuration
    ) // returns Future[ObjectSummaryWithMd5]

  def deleteObject(path: Path.File)(using request: RequestHeader): Future[Unit] = playObjectStoreClient
    .deleteObject(
      path
    ).recover:
      case e => logger.error(s"Failed to delete object $path", e)

  def uploadFromUrl(
    downloadUrl: URL,
    fileName: String
  )(using request: RequestHeader): Future[ObjectSummaryWithMd5] = playObjectStoreClient.uploadFromUrl(
    from = downloadUrl,
    to = receivedResultsFilesPath.file(fileName = fileName),
    retentionPeriod = RetentionPeriod.SixMonths,
    contentType = Some("plain/text")
  )

  def listObjects(using request: RequestHeader): Future[ObjectListing] = playObjectStoreClient.listObjects(receivedResultsFilesPath)

  def generatePreSignedDownloadUrl(
    objectStorePath: Path.Directory,
    objectStoreFileName: String
  )(using request: RequestHeader): Future[PresignedDownloadUrl] = playObjectStoreClient.presignedDownloadUrl(
    path = Path.Directory("applications-for-risking").file(fileName = objectStoreFileName)
  )
