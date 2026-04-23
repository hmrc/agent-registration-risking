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

package uk.gov.hmrc.agentregistrationrisking.runner

import play.api.mvc.Headers
import play.api.mvc.RequestHeader
import play.api.mvc.request.RemoteConnection
import play.api.mvc.request.RequestTarget
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFile
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileId
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileIdGenerator
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.RiskingFileRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.services.ObjectStoreService
import uk.gov.hmrc.agentregistrationrisking.services.RiskingFileService
import uk.gov.hmrc.agentregistrationrisking.services.SdesProxyService
import uk.gov.hmrc.objectstore.client.ObjectSummaryWithMd5
import play.api.libs.typedmap.TypedMap
import uk.gov.hmrc.agentregistrationrisking.util.EmptyRequest
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class RiskingRunner @Inject() (
  objectStoreService: ObjectStoreService,
  riskingFileService: RiskingFileService,
  sdesProxyService: SdesProxyService,
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  individualForRiskingRepo: IndividualForRiskingRepo,
  riskingFileRepo: RiskingFileRepo,
  riskingFileIdGenerator: RiskingFileIdGenerator
)(using
  ec: ExecutionContext,
  clock: Clock
)
extends RequestAwareLogging:

  // outbound flow
  def run(): Future[Unit] =
    given RequestHeader = EmptyRequest.emptyRequestHeader
    val riskingFileId: RiskingFileId = riskingFileIdGenerator.nextRiskingFileId()
    logger.info(s"Running risking started: [$riskingFileId] ...")

    for
      applicationsWithIndividuals <- riskingFileService.getApplicationsReadyForRiskingWithIndividuals
      _ = logger.info(s"Found ${applicationsWithIndividuals.size} applications ready for risking")
      fileContent: String = riskingFileService.buildRiskingFileFrom(applicationsWithIndividuals)
      _ = logger.info("Risking file built successfully")
      objectSummary: ObjectSummaryWithMd5 <- objectStoreService.put(fileContent)
      _ = logger.info(s"File uploaded to object store: ${objectSummary.location}")
      _ <- riskingFileRepo.upsert(RiskingFile(
        _id = riskingFileId,
        fineName = objectSummary.location.fileName,
        uploadedAt = java.time.Instant.now(clock)
      ))
      _ <- applicationForRiskingRepo.updateRiskingFileId(
        ids = applicationsWithIndividuals.map(_.application._id),
        riskingFileId = riskingFileId
      )
      // _ <- Future.traverse(applicationsWithIndividuals)(saveApplicationWithIndividuals(_, riskingFileId))
      _ = logger.info(s"Marked ${applicationsWithIndividuals.size} applications as submitted for risking")
      _ <- sdesProxyService.notifySdesFileReady(objectSummary)
      _ = logger.info(s"SDES notification sent for file: ${objectSummary.location.fileName}")
    yield ()

//  private def markAsSubmittedForRisking(
//    applicationsWithIndividuals: Seq[ApplicationWithIndividuals],
//    riskingFileId: RiskingFileId,
//    fileName: String
//  ): Future[Unit] =
//    for
//      _ <- saveRiskingFile(riskingFileId, fileName)
//      _ <- Future.traverse(applicationsWithIndividuals)(saveApplicationWithIndividuals(_, riskingFileId))
//    yield ()

//  private def saveRiskingFile(
//    riskingFileId: RiskingFileId,
//    fileName: String
//  ): Future[Unit] = riskingFileRepo.upsert(RiskingFile(
//    _id = riskingFileId,
//    fineName = fileName,
//    uploadedAt = java.time.Instant.now(clock)
//  ))

//  private def saveApplicationWithIndividuals(
//    appWithIndividuals: ApplicationWithIndividuals,
//    riskingFileId: RiskingFileId
//  ): Future[Unit] =
//    for
//      _ <- applicationForRiskingRepo.upsert(appWithIndividuals.application.copy(
//        riskingFileId = Some(riskingFileId),
//        lastUpdatedAt = Instant.now(summon[Clock])
//      ))
//      _ <-
//        Future.traverse(appWithIndividuals.individuals)(individual =>
//          individualForRiskingRepo.upsert(individual.copy(riskingFileId = Some(riskingFileId), lastUpdatedAt = Instant.now(summon[Clock])))
//        )
//    yield ()
//  .map(_ => ())
