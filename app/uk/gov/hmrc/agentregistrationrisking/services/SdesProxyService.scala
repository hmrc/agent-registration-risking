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
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationForRiskingStatus
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistration.shared.risking.PersonReference
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.connectors.RiskingResultsConnector
import uk.gov.hmrc.agentregistrationrisking.connectors.SdesProxyConnector
import uk.gov.hmrc.agentregistrationrisking.model.CorrelationIdGenerator
import uk.gov.hmrc.agentregistrationrisking.model.sdes.*
import uk.gov.hmrc.agentregistrationrisking.model.Failure
import uk.gov.hmrc.agentregistrationrisking.model.FailureParser
import uk.gov.hmrc.agentregistrationrisking.model.Result
import uk.gov.hmrc.agentregistrationrisking.model.sdes.AvailableFile
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.model.RiskingRecord
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging
import uk.gov.hmrc.objectstore.client
import uk.gov.hmrc.objectstore.client.ObjectListing
import uk.gov.hmrc.objectstore.client.ObjectSummaryWithMd5
import uk.gov.hmrc.objectstore.client.config.ObjectStoreClientConfig
import uk.gov.hmrc.agentregistration.shared.risking.IndividualRiskingOutcome.*

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
      // TODO: Add file processing logic here
      results: List[Result] = List(
        Result(
          "Entity",
          "1234",
          List(Failure(
            "3.1",
            "some-message",
            "3",
            "some-date",
            None
          ))
        ),
        Result(
          "Individual",
          "1234567890",
          List.empty
//          List(Failure(
//            "4.1",
//            "some-message",
//            "4",
//            "some-date",
//            None
//          ))
        )
      )
      _ <- processResults(results)
      uploadUnprocessedFiles = unprocessedFileList.map(uploadAndLogResultFile)
      uploadResults <- Future.sequence(uploadUnprocessedFiles)
    yield uploadResults

  private def processResults(results: Seq[Result])(using request: RequestHeader): Future[Unit] =
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

  def downloadAndParseRecords(file: AvailableFile)(using request: RequestHeader): Future[List[RiskingRecord]] = {
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

  private def processResult(result: Result)(using request: RequestHeader): Future[Unit] =
    result.recordType match
      case "Entity" => updateEntityWithResult(result)
      case "Individual" => updateIndividualWithResult(result)
      case other =>
        logger.error(s"Skipping unsupported result record type: $other")
        Future.unit

  private def updateEntityWithResult(result: Result)(using request: RequestHeader): Future[Unit] =
    val applicationReference = ApplicationReference(result.reference)
    applicationForRiskingRepo.findById(applicationReference).flatMap:
      case None =>
        logger.error(s"No application found for application reference: ${applicationReference.value}")
        Future.unit
      case Some(applicationForRisking) =>
        val updatedApplication = applicationForRisking.copy(
          failures = Some(result.failures.map(FailureParser.parseEntityFailure))
        )
        logger.info(s"Updated Application: $updatedApplication")
        applicationForRiskingRepo.upsert(updatedApplication)

  private def updateIndividualWithResult(result: Result)(using request: RequestHeader): Future[Unit] =
    val personReference = PersonReference(result.reference)
    applicationForRiskingRepo.findByPersonReference(personReference).flatMap:
      case None =>
        logger.error(s"No individual found for person reference: ${personReference.value}")
        Future.unit
      case Some(applicationForRisking) =>
        val individualFailures: List[IndividualFailure] = result.failures.map(FailureParser.parseIndividualFailure)
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

  private def individualOutcomeAsStatus(individualFailures: List[IndividualFailure]): ApplicationForRiskingStatus =
    individualFailures.outcome match {
      case FailedFixable => ApplicationForRiskingStatus.FailedFixable
      case FailedNonFixable => ApplicationForRiskingStatus.FailedNonFixable
      case Approved => ApplicationForRiskingStatus.Approved
    }
