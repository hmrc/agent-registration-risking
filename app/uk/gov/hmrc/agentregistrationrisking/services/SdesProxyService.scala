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
import uk.gov.hmrc.agentregistration.shared.risking
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationForRiskingStatus
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistration.shared.risking.EntityRiskingOutcome
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistration.shared.risking.IndividualRiskingOutcome
import uk.gov.hmrc.agentregistration.shared.risking.PersonReference
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.connectors.RiskingResultsConnector
import uk.gov.hmrc.agentregistrationrisking.connectors.SdesProxyConnector
import uk.gov.hmrc.agentregistrationrisking.model.CompletedApplicationRiskingOutcome
import uk.gov.hmrc.agentregistrationrisking.model.CorrelationIdGenerator
import uk.gov.hmrc.agentregistrationrisking.model.Failure
import uk.gov.hmrc.agentregistrationrisking.model.FailureParser
import uk.gov.hmrc.agentregistrationrisking.model.RiskingResultRecord
import uk.gov.hmrc.agentregistrationrisking.model.sdes.*
import uk.gov.hmrc.agentregistrationrisking.model.sdes.AvailableFile
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging
import uk.gov.hmrc.objectstore.client
import uk.gov.hmrc.objectstore.client.ObjectListing
import uk.gov.hmrc.objectstore.client.ObjectSummaryWithMd5
import uk.gov.hmrc.objectstore.client.config.ObjectStoreClientConfig
import uk.gov.hmrc.agentregistration.shared.risking.IndividualRiskingOutcome.*
import uk.gov.hmrc.agentregistration.shared.risking.EntityRiskingOutcome.*

import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class SdesProxyService @Inject() (
  sdesProxyConnector: SdesProxyConnector,
  objectStoreService: ObjectStoreService,
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  appConfig: AppConfig,
  objectStoreClientConfig: ObjectStoreClientConfig,
  riskingResultsConnector: RiskingResultsConnector,
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
    // I'm really not sure the best way to handle this url, it seems to come from the object store config
    // which is set automatically, is there a cleaner way to do this?
    val location = s"${objectStoreClientConfig.baseUrl}/object-store/object/${objectStoreClientConfig.owner}/${objectSummaryWithMd5.location.directory.asUri}"
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

  def retrieveAndProcessResultsFiles(using request: RequestHeader): Future[Seq[ObjectSummaryWithMd5]] =
    logger.info(s"Results file retrieval started...")
    for
      unprocessedFileList <- getUnprocessedAvailableFiles()
      _ = logger.info(s"Unprocessed files found: ${unprocessedFileList.size}")
      uploadResults <-
        unprocessedFileList.foldLeft(Future.successful(Seq.empty[ObjectSummaryWithMd5])):
          (
            processedFiles,
            resultsFile
          ) =>
            processedFiles.flatMap: completed =>
              for
                records <- downloadAndParseRecords(resultsFile)
                _ <- processResults(records)
                completedOutcomes <- syncApplicationStatuses(records)
                uploadResult <- uploadAndLogResultFile(resultsFile)
              yield completed :+ uploadResult
    yield uploadResults

  private def processResults(results: List[RiskingResultRecord])(using request: RequestHeader): Future[Unit] =
    // Process in order so repeated updates to the same application do not race.
    results.foldLeft(Future.unit): (acc, result) =>
      acc.flatMap(_ => processResult(result))

  private def uploadAndLogResultFile(file: AvailableFile)(using request: RequestHeader): Future[ObjectSummaryWithMd5] =
    val downloadUri = Uri(file.downloadURL).toJavaUri.toURL
    val fileName = file.filename
    for
      uploadResult <- objectStoreService.uploadFromUrl(downloadUrl = downloadUri, fileName = fileName)
      _ = logger.info(s"Uploaded file to object store: $fileName")
    yield uploadResult

  def downloadAndParseRecords(file: AvailableFile)(using request: RequestHeader): Future[List[RiskingResultRecord]] = {
    logger.info(s"Attempting to downloading ${file.filename}")
    riskingResultsConnector.getRiskingFile(availableFile = file)
  }

  private def getUnprocessedAvailableFiles()(using request: RequestHeader): Future[Seq[AvailableFile]] =
    for
      availableFiles: Seq[AvailableFile] <- sdesProxyConnector.listAvailableFiles
      filesAlreadyProcessed: ObjectListing <- objectStoreService.listObjects
      _ = logger.info(s"Files already processed: ${filesAlreadyProcessed.objectSummaries.size}")
      fileNamesAlreadyProcessed = filesAlreadyProcessed.objectSummaries.map(_.location.fileName).toSet
      _ = logger.info(s"File Names already processed: ${fileNamesAlreadyProcessed.toString}")
      unprocessedAvailableFiles = availableFiles.filterNot(f => fileNamesAlreadyProcessed.contains(f.filename))
    yield unprocessedAvailableFiles

  private def processResult(result: RiskingResultRecord)(using request: RequestHeader): Future[Unit] =
    result.recordType match
      case "Entity" => updateEntityWithResult(result)
      case "Individual" => updateIndividualWithResult(result)
      case other =>
        logger.error(s"Skipping unsupported result record type: $other")
        Future.unit

  private def updateEntityWithResult(result: RiskingResultRecord)(using request: RequestHeader): Future[Unit] =
    result.applicationReference match
      case None =>
        logger.error("Entity result record missing application reference, cannot update application")
        Future.unit
      case Some(applicationReference) =>
        applicationForRiskingRepo.findById(applicationReference).flatMap:
          case None =>
            logger.error(s"No application found for application reference: ${applicationReference.value}")
            Future.unit
          case Some(applicationForRisking) =>
            val updatedApplication = applicationForRisking.copy(
              failures = Some(result.failures.getOrElse(List.empty).map(FailureParser.parseEntityFailure))
            )
            logger.info(s"Updated Application: $updatedApplication")
            applicationForRiskingRepo.upsert(updatedApplication)

  private def updateIndividualWithResult(result: RiskingResultRecord)(using request: RequestHeader): Future[Unit] =
    result.personReference match
      case None =>
        logger.error("Individual result record missing person reference, cannot update application")
        Future.unit
      case Some(personReference) =>
        applicationForRiskingRepo.findByPersonReference(personReference).flatMap:
          case None =>
            logger.error(s"No individual found for person reference: ${personReference.value}")
            Future.unit
          case Some(applicationForRisking) =>
            val individualFailures = result.failures.getOrElse(List.empty).map(FailureParser.parseIndividualFailure)
            val updatedIndividuals = applicationForRisking.individuals.map:
              case individual if individual.personReference.value === personReference.value =>
                individual.copy(
                  failures = Some(individualFailures),
                  status = individualOutcomeAsStatus(individualFailures)
                )
              case individual => individual

            val updated = applicationForRisking.copy(
              individuals = updatedIndividuals
            )
            applicationForRiskingRepo.upsert(updated)

  def syncApplicationStatuses(results: List[RiskingResultRecord])(using RequestHeader): Future[Set[CompletedApplicationRiskingOutcome]] =
    for
      applicationReferences <- getProcessedApplicationReferences(results)
      completedOutcomes <- getCompletedCompletedApplicationRiskingOutcomes(applicationReferences)
      _ <- updateApplicationStatuses(completedOutcomes)
    yield (completedOutcomes)

  private def individualOutcomeAsStatus(individualFailures: List[IndividualFailure]): ApplicationForRiskingStatus =
    individualFailures.outcome() match {
      case IndividualRiskingOutcome.FailedFixable => ApplicationForRiskingStatus.FailedFixable
      case IndividualRiskingOutcome.FailedNonFixable => ApplicationForRiskingStatus.FailedNonFixable
      case IndividualRiskingOutcome.Approved => ApplicationForRiskingStatus.Approved
    }

  private def getProcessedApplicationReferences(results: List[RiskingResultRecord]): Future[Set[ApplicationReference]] =
    val (entityResults, individualResults) = results.partition(_.recordType === "Entity")
    val entityRefs: Set[ApplicationReference] = entityResults.flatMap(_.applicationReference).toSet
    val individualRefsFuture: Future[Set[ApplicationReference]] = Future.traverse(individualResults.flatMap(_.personReference)): personReference =>
      applicationForRiskingRepo.findByPersonReference(personReference).map(_.map(_.applicationReference))
    .map(_.flatten.toSet)
    individualRefsFuture.map(entityRefs ++ _)

  private def getCompletedCompletedApplicationRiskingOutcomes(applicationReferences: Set[ApplicationReference]): Future[
    Set[CompletedApplicationRiskingOutcome]
  ] = Future.traverse(applicationReferences)(getCompletedCompletedApplicationRiskingOutcome).map(_.flatten)

  private def getCompletedCompletedApplicationRiskingOutcome(applicationReference: ApplicationReference): Future[Option[CompletedApplicationRiskingOutcome]] =
    applicationForRiskingRepo.findById(applicationReference).map:
      _.flatMap: application =>
        val completedIndividualStatuses = application.individuals.map(_.status).map(asCompleted)

        Option.when(completedIndividualStatuses.forall(_.isDefined))(application.failures).flatten.map: entityFailures =>
          CompletedApplicationRiskingOutcome(
            applicationReference = applicationReference,
            entityStatus = entityOutcomeAsStatus(entityFailures),
            individualStatuses = completedIndividualStatuses.flatten
          )

  private def entityOutcomeAsStatus(entityFailures: List[EntityFailure]): ApplicationForRiskingStatus.CompletedStatus =
    entityFailures.outcome() match
      case EntityRiskingOutcome.FailedFixable => ApplicationForRiskingStatus.FailedFixable
      case EntityRiskingOutcome.FailedNonFixable => ApplicationForRiskingStatus.FailedNonFixable
      case EntityRiskingOutcome.Approved => ApplicationForRiskingStatus.Approved

  private def updateApplicationStatuses(
    outcomes: Set[CompletedApplicationRiskingOutcome]
  )(using RequestHeader): Future[Unit] = Future.traverse(outcomes)(updateApplicationStatus).map(_ => ())

  private def updateApplicationStatus(
    outcome: CompletedApplicationRiskingOutcome
  )(using RequestHeader): Future[Unit] = applicationForRiskingRepo.findById(outcome.applicationReference).flatMap:
    case None =>
      logger.error(s"No application found for application reference: ${outcome.applicationReference.value}")
      Future.unit
    case Some(applicationForRisking) =>
      val updatedApplication = applicationForRisking.copy(status = outcome.applicationStatus)
      applicationForRiskingRepo.upsert(updatedApplication)

  private def asCompleted(status: ApplicationForRiskingStatus): Option[ApplicationForRiskingStatus.CompletedStatus] =
    status match
      case s @ (ApplicationForRiskingStatus.Approved | ApplicationForRiskingStatus.FailedNonFixable | ApplicationForRiskingStatus.FailedFixable) => Some(s)
      case _ => None
