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

package uk.gov.hmrc.agentregistrationrisking.testonly.services

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistrationrisking.util.ProcessInSequence
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationrisking.util.RequestSupport.hc
import uk.gov.hmrc.objectstore.client.ObjectListing
import uk.gov.hmrc.objectstore.client.Path
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class TestOnlyObjectStoreService @Inject() (
  playObjectStoreClient: PlayObjectStoreClient
)(using ExecutionContext)
extends RequestAwareLogging:

  private val receivedResultsFilesPath = Path.Directory("processed-results-files")

  def deleteProcessedResultsFiles()(using request: RequestHeader): Future[Unit] =
    for
      objectListing: ObjectListing <- playObjectStoreClient.listObjects(receivedResultsFilesPath)
      _ = logger.info(
        s"Deleting ${objectListing.objectSummaries.size} objects from processed-results-files:...\n ${objectListing.objectSummaries.mkString("\n")}"
      )
      deletedCount <-
        ProcessInSequence.processAllInSequence(objectListing.objectSummaries)(objectSummary =>
          playObjectStoreClient
            .deleteObject(objectSummary.location)
            .map(_ => logger.info(s"Deleted ${objectSummary}"))
        ):
          case (throwable, item) => logger.error(s"Failed to delete $item", throwable)
      _ = logger.info(s"Deleted $deletedCount of ${objectListing.objectSummaries.size} objects from processed-results-files")
    yield ()
