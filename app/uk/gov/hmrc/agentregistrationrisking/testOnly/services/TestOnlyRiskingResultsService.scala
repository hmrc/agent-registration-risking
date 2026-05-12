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

package uk.gov.hmrc.agentregistrationrisking.testOnly.services

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistrationrisking.connectors.RiskingResultsFileConnector
import uk.gov.hmrc.agentregistrationrisking.connectors.SdesProxyConnector
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.EntityRiskingResult
import uk.gov.hmrc.agentregistrationrisking.model.IndividualRiskingResult
import uk.gov.hmrc.agentregistrationrisking.model.RiskingResult
import uk.gov.hmrc.agentregistrationrisking.model.RiskingResultParser
import uk.gov.hmrc.agentregistrationrisking.model.sdes.AvailableFile
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.services.ObjectStoreService
import uk.gov.hmrc.agentregistrationrisking.util.ProcessInSequence
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging
import uk.gov.hmrc.objectstore.client.ObjectListing

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class TestOnlyRiskingResultsService @Inject() (
  sdesProxyConnector: SdesProxyConnector,
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  individualForRiskingRepo: IndividualForRiskingRepo,
  objectStoreService: ObjectStoreService,
  riskingResultsFileConnector: RiskingResultsFileConnector,
  clock: Clock
)(using ExecutionContext)
extends RequestAwareLogging:

  def processResultsFilesSkipUpload()(using request: RequestHeader): Future[Unit] =
    logger.info(s"Processing RiskingResultsFile(s) (test-only skip-upload mode) ...")
    for
      unprocessedAvailableFiles <- getUnprocessedAvailableFiles()
      _ = logger.info(s"Found ${unprocessedAvailableFiles.size} RiskingResultsFile(s) to process")
      numberOfProcessedFiles <- ProcessInSequence.processInSequence(unprocessedAvailableFiles)(processResultsFileSkipUpload).map(_.size)
      _ = logger.info(s"Processing RiskingResultsFile(s) COMPLETE: $numberOfProcessedFiles")
    yield ()

  private def processResultsFileSkipUpload(availableFile: AvailableFile)(using request: RequestHeader): Future[Unit] =
    logger.info(s"Processing RiskingResultsFile (skip-upload): ${availableFile.filename}, size:${availableFile.fileSize}...")
    for
      riskingResults <- riskingResultsFileConnector
        .getRiskingResultRecords(availableFile = availableFile)
        .map(_.map(RiskingResultParser.parseRiskingResult))
      _ = logger.info(s"Downloaded and parsed risking results file: ${availableFile.filename} (${riskingResults.size} records)")
      numberOfUpdates <- ProcessInSequence.processInSequence(riskingResults)(processRiskingResult).map(_.size)
      _ = logger.info(s"Updated matching $numberOfUpdates applications and individuals with retrieved risking results")
    yield ()

  private def getUnprocessedAvailableFiles()(using request: RequestHeader): Future[Seq[AvailableFile]] =
    for
      availableFiles <- sdesProxyConnector.listAvailableFiles
      filesAlreadyProcessed: ObjectListing <- objectStoreService.listObjects
      fileNamesAlreadyProcessed = filesAlreadyProcessed.objectSummaries.map(_.location.fileName).toSet
      unprocessedAvailableFiles = availableFiles.filterNot(f => fileNamesAlreadyProcessed.contains(f.filename))
    yield unprocessedAvailableFiles

  private def processRiskingResult(riskingResult: RiskingResult)(using request: RequestHeader): Future[Unit] =
    riskingResult match
      case entity: RiskingResult.ForEntity => updateEntity(entity)
      case individual: RiskingResult.ForIndividual => updateIndividual(individual)

  private def updateEntity(riskingResult: RiskingResult.ForEntity)(using request: RequestHeader): Future[Unit] = applicationForRiskingRepo
    .findById(riskingResult.applicationReference)
    .flatMap:
      case None =>
        logger.error(s"Missing application for: ${riskingResult.applicationReference}")
        Future.unit
      case Some(application) =>
        val now = Instant.now(clock)
        val updatedApplication: ApplicationForRisking = application.copy(
          entityRiskingResult = Some(EntityRiskingResult(failures = riskingResult.failures, receivedAt = now)),
          lastUpdatedAt = now
        )
        applicationForRiskingRepo.upsert(updatedApplication).map(_ => ())

  private def updateIndividual(riskingResult: RiskingResult.ForIndividual)(using request: RequestHeader): Future[Unit] = individualForRiskingRepo
    .findById(riskingResult.personReference)
    .flatMap:
      case None =>
        logger.error(s"Missing individual for: ${riskingResult.personReference}")
        Future.unit
      case Some(individual) =>
        val now = Instant.now(clock)
        val updatedIndividual: IndividualForRisking = individual.copy(
          individualRiskingResult = Some(IndividualRiskingResult(failures = riskingResult.failures, receivedAt = now)),
          lastUpdatedAt = now
        )
        individualForRiskingRepo.upsert(updatedIndividual).map(_ => ())
