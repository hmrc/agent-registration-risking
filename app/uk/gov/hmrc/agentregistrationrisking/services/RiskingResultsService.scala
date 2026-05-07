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
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.connectors.RiskingResultsFileConnector
import uk.gov.hmrc.agentregistrationrisking.connectors.SdesProxyConnector
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.model.CorrelationIdGenerator
import uk.gov.hmrc.agentregistrationrisking.model.EntityRiskingResult
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualRiskingResult
import uk.gov.hmrc.agentregistrationrisking.model.RiskingResult
import uk.gov.hmrc.agentregistrationrisking.model.RiskingResultParser
import uk.gov.hmrc.agentregistrationrisking.model.sdes.*
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.util.ProcessInSequence
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationrisking.util.Utils.*
import uk.gov.hmrc.objectstore.client.Md5Hash
import uk.gov.hmrc.objectstore.client.ObjectListing
import uk.gov.hmrc.objectstore.client.ObjectSummaryWithMd5
import uk.gov.hmrc.objectstore.client.config.ObjectStoreClientConfig

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import RiskingOutcomeHelper.*

@Singleton
class RiskingResultsService @Inject() (
  sdesProxyConnector: SdesProxyConnector,
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  individualForRiskingRepo: IndividualForRiskingRepo,
  appConfig: AppConfig,
  objectStoreClientConfig: ObjectStoreClientConfig,
  objectStoreService: ObjectStoreService,
  correlationIdGenerator: CorrelationIdGenerator,
  riskingResultsFileConnector: RiskingResultsFileConnector,
  clock: Clock
)(using
  ExecutionContext
)
extends RequestAwareLogging:

  def processResultsFiles()(using request: RequestHeader): Future[Unit] =
    logger.info(s"Processing RiskingResultsFile(s) ...")
    for
      unprocessedAvailableFiles: Seq[AvailableFile] <- getUnprocessedAvailableFiles()
      _ = logger.info(s"Found ${unprocessedAvailableFiles.size} RiskingResultsFile(s) to process")
      numberOfProcessedFiles <- ProcessInSequence.processInSequence(unprocessedAvailableFiles)(processResultsFile).map(_.size)
      _ = logger.info(s"Processing RiskingResultsFile(s) COMPLETE: $numberOfProcessedFiles")
    yield ()

  private def processResultsFile(availableFile: AvailableFile)(using request: RequestHeader): Future[Unit] =
    logger.info(s"Processing RiskingResultsFile: ${availableFile.filename}, size:${availableFile.fileSize}...")
    for
      riskingResults: Seq[RiskingResult] <- riskingResultsFileConnector
        .getRiskingResultRecords(availableFile = availableFile)
        .map(_.map(RiskingResultParser.parseRiskingResult))
      _ = logger.info(s"Downloaded and parsed risking results file: ${availableFile.filename} (${riskingResults.size} records)")
      numberOfUpdates <- ProcessInSequence.processInSequence(riskingResults)(processRiskingResult).map(_.size)
      _ = logger.info(s"Updated matching $numberOfUpdates applications and individuals with retrieved risking results")
      uploadResult: ObjectSummaryWithMd5 <- uploadRiskingResultFileToObjectStoreForBackup(availableFile)
      _ = logger.info(s"Uploaded RiskingResultsFile to object store as backup and evidence: $uploadResult")
    yield ()

  private def uploadRiskingResultFileToObjectStoreForBackup(file: AvailableFile)(using request: RequestHeader): Future[ObjectSummaryWithMd5] =
    val downloadUri = Uri(file.downloadURL).toJavaUri.toURL
    val fileName = file.filename
    for
      uploadResult <- objectStoreService.uploadRiskingResultsFileFromUrl(downloadUrl = downloadUri, fileName = fileName)
      _ = logger.info(s"Uploaded file to object store: $fileName")
    yield uploadResult

  private def getUnprocessedAvailableFiles()(using request: RequestHeader): Future[Seq[AvailableFile]] =
    for
      availableFiles: Seq[AvailableFile] <- sdesProxyConnector.listAvailableFiles
      filesAlreadyProcessed: ObjectListing <- objectStoreService.listObjects
      _ = logger.info(s"Files already processed: ${filesAlreadyProcessed.objectSummaries.size}")
      fileNamesAlreadyProcessed = filesAlreadyProcessed.objectSummaries.map(_.location.fileName).toSet
      _ = logger.info(s"File Names already processed: ${fileNamesAlreadyProcessed.toString}")
      unprocessedAvailableFiles = availableFiles.filterNot(f => fileNamesAlreadyProcessed.contains(f.filename))
    yield unprocessedAvailableFiles

  private def processRiskingResult(riskingResult: RiskingResult)(using request: RequestHeader): Future[Unit] =
    riskingResult match
      case riskingResult: RiskingResult.ForEntity => updateRiskingResults(riskingResult)
      case riskingResult: RiskingResult.ForIndividual => updateRiskingResults(riskingResult)

  private def updateRiskingResults(riskingResult: RiskingResult.ForEntity)(using request: RequestHeader): Future[Unit] = applicationForRiskingRepo
    .findById(riskingResult.applicationReference)
    .flatMap:
      case None =>
        // TODO: audit event needed
        logger.error(s"Missing application for: ${riskingResult.applicationReference}")
        Future.unit
      case Some(application) =>
        val now = Instant.now(clock)
        val updatedApplication: ApplicationForRisking = application.copy(
          entityRiskingResult = Some(EntityRiskingResult(failures = riskingResult.failures, receivedAt = now)),
          lastUpdatedAt = now
        )
        applicationForRiskingRepo
          .upsert(updatedApplication)
          .map: _ =>
            logger.debug(s"Updated Application with risking results: ${updatedApplication.applicationReference} (${riskingResult.failures.outcomeForEntity})")

  private def updateRiskingResults(riskingResult: RiskingResult.ForIndividual)(using request: RequestHeader): Future[Unit] = individualForRiskingRepo
    .findById(riskingResult.personReference)
    .flatMap:
      case None =>
        // TODO: audit event needed
        logger.error(s"Missing individual for: ${riskingResult.personReference}")
        Future.unit
      case Some(individual) =>
        val now = Instant.now(clock)
        val updatedIndividual: IndividualForRisking = individual.copy(
          individualRiskingResult = Some(IndividualRiskingResult(failures = riskingResult.failures, receivedAt = now)),
          lastUpdatedAt = now
        )
        individualForRiskingRepo
          .upsert(updatedIndividual)
          .map: _ =>
            logger.debug(s"Updated Individual with risking results: ${updatedIndividual.personReference} (${riskingResult.failures.outcome})")
