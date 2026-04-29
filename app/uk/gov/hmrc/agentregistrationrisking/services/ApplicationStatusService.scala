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
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcome
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcome.*
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.model.EntityRiskingResultRecord
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualRiskingResultRecord
import uk.gov.hmrc.agentregistrationrisking.model.RiskingResultRecord
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ApplicationStatusService @Inject() (
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  individualForRiskingRepo: IndividualForRiskingRepo
)(using
  ExecutionContext,
  Clock
)
extends RequestAwareLogging:

  def getAllUnsubscribedApplicationsWithIndividualsWithResults: Future[Seq[ApplicationWithIndividuals]] =
    for
      applications: Seq[ApplicationForRisking] <- applicationForRiskingRepo.findNotSubscribedWithResults()
      applicationsWithIndividuals: Seq[Option[ApplicationWithIndividuals]] <-
        Future.traverse(applications): (application: ApplicationForRisking) =>
          individualForRiskingRepo.findByApplicationReference(application.applicationReference)
            .map(individuals => ApplicationWithIndividuals(application, individuals))
            .map: appWithIndividuals =>
              val allIndividualsReceived: Boolean = appWithIndividuals.individuals.forall(_.failures.isDefined)
              if allIndividualsReceived then Some(appWithIndividuals) else None
    yield applicationsWithIndividuals.flatten

  def getApprovedApplicationsWithIndividuals(applicationsWithIndividuals: Seq[ApplicationWithIndividuals]): Seq[ApplicationWithIndividuals] =
    applicationsWithIndividuals.filter: appWithIndividuals =>
      val entityApproved: Boolean = appWithIndividuals.application.failures.exists(_.outcomeForEntity === RiskingOutcome.Approved)
      val allIndividualsApproved: Boolean = appWithIndividuals.individuals.forall: individual =>
        individual.failures.exists(_.outcome === RiskingOutcome.Approved)
      entityApproved && allIndividualsApproved

  def processResults(riskingResultRecords: List[RiskingResultRecord])(using request: RequestHeader): Future[Unit] =
    // Process in order so repeated updates to the same application do not race.
    riskingResultRecords.foldLeft(Future.unit): (acc, result) =>
      acc.flatMap(_ => processResult(result))

  private def processResult(riskingResultRecords: RiskingResultRecord)(using request: RequestHeader): Future[Unit] =
    riskingResultRecords match
      case entityRiskingResultRecord: EntityRiskingResultRecord => updateEntityWithResult(entityRiskingResultRecord)
      case individualRiskingResultRecord: IndividualRiskingResultRecord => updateIndividualWithResult(individualRiskingResultRecord)

  private def updateEntityWithResult(entityRiskingResultRecord: EntityRiskingResultRecord)(using request: RequestHeader): Future[Unit] =
    applicationForRiskingRepo.findById(entityRiskingResultRecord.applicationReference).flatMap:
      case None =>
        logger.error(s"No application found for application reference: ${entityRiskingResultRecord.applicationReference.value}")
        Future.unit
      case Some(applicationForRisking) =>
        val updatedApplication = applicationForRisking.copy(
          failures = Some(entityRiskingResultRecord.failures),
          lastUpdatedAt = Instant.now(summon[Clock])
        )
        logger.info(s"Updated Application: $updatedApplication")
        applicationForRiskingRepo.upsert(updatedApplication)

  private def updateIndividualWithResult(individualRiskingResultRecord: IndividualRiskingResultRecord)(using request: RequestHeader): Future[Unit] =
    individualForRiskingRepo.findById(individualRiskingResultRecord.personReference).flatMap:
      case None =>
        logger.error(s"No individual found for person reference: ${individualRiskingResultRecord.personReference.value}")
        Future.unit
      case Some(individualForRisking) =>
        val updatedIndividual = individualForRisking.copy(
          failures = Some(individualRiskingResultRecord.failures),
          lastUpdatedAt = Instant.now(summon[Clock])
        )
        logger.info(s"Updated Individual: $updatedIndividual")
        individualForRiskingRepo.upsert(updatedIndividual)
