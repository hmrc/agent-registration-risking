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

package uk.gov.hmrc.agentregistrationrisking.controllers

import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistration.shared.risking.RiskedEntity
import uk.gov.hmrc.agentregistration.shared.risking.RiskedIndividual
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcome
import uk.gov.hmrc.agentregistration.shared.risking.RiskingProgress
import uk.gov.hmrc.agentregistration.shared.risking.RiskingProgress.ReceivedRiskingResults
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.=!=
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationrisking.action.Actions
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileName
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.services.ApplicationForRiskingService
import uk.gov.hmrc.agentregistrationrisking.services.RiskingOutcomeHelper
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcome.*

import java.time.LocalDate
import java.time.Clock
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class RiskingProgressController @Inject() (
  actions: Actions,
  cc: ControllerComponents,
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  individualForRiskingRepo: IndividualForRiskingRepo,
  applicationForRiskingService: ApplicationForRiskingService,
  clock: Clock
)(using ExecutionContext)
extends BackendController(cc):

  def getRiskingProgressForIndividual(personReference: PersonReference) = actions
    .authorised
    .async:
      applicationForRiskingService
        .getApplicationWithIndividuals(personReference)
        .map:
          case None => NoContent
          case Some(applicationWithIndividuals) =>
            Ok(Json.toJson(
              RiskingProgressController.toRiskingProgress(
                applicationWithIndividuals = applicationWithIndividuals,
                riskingCompletedDate = LocalDate.now(clock) // TODO! This has to come from DB
              )
            ))

  def getRiskingProgressForApplicant(applicationReference: ApplicationReference): Action[AnyContent] = actions
    .authorised
    .async:
      applicationForRiskingService
        .getApplicationWithIndividuals(applicationReference)
        .map:
          case None => NoContent
          case Some(applicationWithIndividuals: ApplicationWithIndividuals) =>
            Ok(Json.toJson(
              RiskingProgressController.toRiskingProgress(
                applicationWithIndividuals = applicationWithIndividuals,
                riskingCompletedDate = LocalDate.now(clock) // TODO! This has to come from DB
              )
            ))

object RiskingProgressController:

  def toRiskingProgress(
    applicationWithIndividuals: ApplicationWithIndividuals,
    riskingCompletedDate: LocalDate
  ): RiskingProgress =
    val maybeRiskingFileName: Option[RiskingFileName] = applicationWithIndividuals.application.riskingFileName
    val maybeReceivedRiskingResults: Option[ReceivedRiskingResults] = receivedRiskingResults(applicationWithIndividuals, riskingCompletedDate)

    (maybeRiskingFileName, maybeReceivedRiskingResults) match
      // format: off
      case (None,    _                           ) => RiskingProgress.ReadyForSubmission
      case (Some(_), None                        ) => RiskingProgress.SubmittedForRisking
      case (Some(_), Some(receivedRiskingResults)) => receivedRiskingResults
      // format: on

  private def receivedRiskingResults(
    applicationWithIndividuals: ApplicationWithIndividuals,
    riskingCompletedDate: LocalDate
  ): Option[ReceivedRiskingResults] =
    import cats.implicits._
    for
      outcome: RiskingOutcome <- RiskingOutcomeHelper.computeRiskingOutcome(applicationWithIndividuals)
      riskedEntity: RiskedEntity <- applicationWithIndividuals
        .application
        .failures.map: failures =>
          RiskedEntity(
            applicationReference = applicationWithIndividuals.application.applicationReference,
            failures = failures
          )
      riskedIndividuals: Seq[RiskedIndividual] <-
        applicationWithIndividuals
          .individuals
          .map: individual =>
            individual.failures.map: (failures: Seq[IndividualFailure]) =>
              RiskedIndividual(
                personReference = individual.personReference,
                individualName = individual.individualProvidedDetails.individualName,
                failures = failures
              )
          .sequence
    yield outcome match
      case Approved => RiskingProgress.Approved
      case FailedFixable =>
        RiskingProgress.FailedFixable(
          riskedEntity = riskedEntity,
          riskedIndividuals = riskedIndividuals,
          riskingCompletedDate = riskingCompletedDate
        )
      case FailedNonFixable =>
        RiskingProgress.FailedNonFixable(
          riskedEntity = riskedEntity,
          riskedIndividuals = riskedIndividuals,
          riskingCompletedDate = riskingCompletedDate
        )

  private def maybeRiskedEntity(applicationWithIndividuals: ApplicationWithIndividuals): Option[RiskedEntity] = applicationWithIndividuals
    .application
    .failures
    .map((failures: List[EntityFailure]) => RiskedEntity(applicationWithIndividuals.application.applicationReference, failures))

  private def maybeRiskedIndividuals(applicationWithIndividuals: ApplicationWithIndividuals): Option[Seq[RiskedIndividual]] =
    import cats.implicits._
    applicationWithIndividuals
      .individuals
      .map: individual =>
        individual
          .failures
          .map(failures =>
            RiskedIndividual(
              personReference = individual.individualProvidedDetails.personReference,
              individualName = individual.individualProvidedDetails.individualName,
              failures = failures
            )
          )
      .sequence
