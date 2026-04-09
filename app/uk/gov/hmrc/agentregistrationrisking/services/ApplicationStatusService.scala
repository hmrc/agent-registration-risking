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
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationForRiskingStatus
import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistration.shared.risking.EntityRiskingOutcome
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistration.shared.risking.IndividualRiskingOutcome
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.CompletedApplicationRiskingOutcome
import uk.gov.hmrc.agentregistrationrisking.model.FailureParser
import uk.gov.hmrc.agentregistrationrisking.model.RiskingResultRecord
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging
import uk.gov.hmrc.agentregistration.shared.risking.EntityRiskingOutcome.*
import uk.gov.hmrc.agentregistration.shared.risking.IndividualRiskingOutcome.*

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ApplicationStatusService @Inject() (
  applicationForRiskingRepo: ApplicationForRiskingRepo
)(using ExecutionContext)
extends RequestAwareLogging:

  def processResults(results: List[RiskingResultRecord])(using request: RequestHeader): Future[Unit] =
    // Process in order so repeated updates to the same application do not race.
    results.foldLeft(Future.unit): (acc, result) =>
      acc.flatMap(_ => processResult(result))

  def updateApplicationStatuses(results: List[RiskingResultRecord])(using RequestHeader): Future[Set[ApplicationForRisking]] =
    for
      applications <- getProcessedApplications(results)
      completedOutcomes = applications.flatMap(getCompletedApplicationRiskingOutcome)
      updatedApplications <- updateApplicationStatuses(completedOutcomes)
    yield updatedApplications

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

  private def getProcessedApplications(results: List[RiskingResultRecord])(using RequestHeader): Future[Set[ApplicationForRisking]] =
    val (entityResults, individualResults) = results.partition(_.recordType === "Entity")
    val entityAppsFuture: Future[Set[ApplicationForRisking]] = Future.traverse(entityResults.flatMap(_.applicationReference).toSet): appRef =>
      applicationForRiskingRepo.findById(appRef)
        .recover:
          case ex: Throwable =>
            logger.error(s"Failed to find application for reference ${appRef.value}: ${ex.getMessage}")
            None
    .map(_.flatten)
    val individualAppsFuture: Future[Set[ApplicationForRisking]] = Future.traverse(individualResults.flatMap(_.personReference).toSet): personReference =>
      applicationForRiskingRepo.findByPersonReference(personReference)
        .recover:
          case ex: Throwable =>
            logger.error(s"Failed to find application for person reference ${personReference.value}: ${ex.getMessage}")
            None
    .map(_.flatten)
    for
      entityApps <- entityAppsFuture
      individualApps <- individualAppsFuture
    yield entityApps ++ individualApps

  private def getCompletedApplicationRiskingOutcome(application: ApplicationForRisking): Option[CompletedApplicationRiskingOutcome] =
    val allIndividualsCompleted = application.individuals.map(_.status).forall:
      case _: ApplicationForRiskingStatus.RiskingCompletedStatus => true
      case _ => false

    application.failures match
      case Some(entityFailures) if allIndividualsCompleted =>
        Some(CompletedApplicationRiskingOutcome(
          application = application,
          entityStatus = entityOutcomeAsStatus(entityFailures)
        ))
      case _ => None

  private def entityOutcomeAsStatus(entityFailures: List[EntityFailure]): ApplicationForRiskingStatus.RiskingCompletedStatus =
    entityFailures.outcome() match
      case EntityRiskingOutcome.FailedFixable => ApplicationForRiskingStatus.FailedFixable
      case EntityRiskingOutcome.FailedNonFixable => ApplicationForRiskingStatus.FailedNonFixable
      case EntityRiskingOutcome.Approved => ApplicationForRiskingStatus.Approved

  private def individualOutcomeAsStatus(individualFailures: List[IndividualFailure]): ApplicationForRiskingStatus =
    individualFailures.outcome() match
      case IndividualRiskingOutcome.FailedFixable => ApplicationForRiskingStatus.FailedFixable
      case IndividualRiskingOutcome.FailedNonFixable => ApplicationForRiskingStatus.FailedNonFixable
      case IndividualRiskingOutcome.Approved => ApplicationForRiskingStatus.Approved

  private def updateApplicationStatuses(
    outcomes: Set[CompletedApplicationRiskingOutcome]
  )(using RequestHeader): Future[Set[ApplicationForRisking]] =
    val updatedApplications = outcomes.map(a => a.application.copy(status = a.applicationStatus))
    Future.traverse(updatedApplications): application =>
      applicationForRiskingRepo.upsert(application)
        .recover:
          case ex: Throwable => logger.error(s"Failed to update application status for ${application.applicationReference.value}: ${ex.getMessage}")
    .map(_ => updatedApplications)
