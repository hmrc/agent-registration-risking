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

import uk.gov.hmrc.agentregistration.shared.risking.*
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking

import java.time.LocalDate

object RiskingOutcomeHelper:

  def computeRiskingOutcome(applicationWithIndividuals: ApplicationWithIndividuals): Option[RiskingOutcome] =
    for
      entityOutcome: RiskingOutcome <- applicationWithIndividuals
        .application
        .failures
        .map(_.outcomeForEntity)
      individualsOutcome: RiskingOutcome <- maybeOutcomeForIndividuals(applicationWithIndividuals.individuals)
    yield foldOutcomes(entityOutcome, individualsOutcome)

  def maybeOutcomeForIndividuals(individuals: Seq[IndividualForRisking]): Option[RiskingOutcome] =
    import cats.implicits.*
    individuals
      .map(_.failures.map(_.outcome))
      .sequence
      .map(_.foldLeft[RiskingOutcome](RiskingOutcome.Approved)(foldOutcomes))

  extension (failure: IndividualFailure)
    def asOutcome: RiskingOutcome =
      failure match
        case _: IndividualFailure.Fixable => RiskingOutcome.FailedFixable
        case _: IndividualFailure.NonFixable => RiskingOutcome.FailedNonFixable

  extension (failure: EntityFailure)
    def asOutcome: RiskingOutcome =
      failure match
        case _: EntityFailure.Fixable => RiskingOutcome.FailedFixable
        case _: EntityFailure.NonFixable => RiskingOutcome.FailedNonFixable

  extension (failures: List[IndividualFailure])
    def outcome: RiskingOutcome =
      failures
        .map(asOutcome)
        .foldLeft(RiskingOutcome.Approved)(foldOutcomes)

  extension (failures: List[EntityFailure])
    def outcomeForEntity: RiskingOutcome =
      failures
        .map(asOutcome)
        .foldLeft[RiskingOutcome](RiskingOutcome.Approved)(foldOutcomes)

  def foldOutcomes(
    o1: RiskingOutcome,
    o2: RiskingOutcome
  ): RiskingOutcome =
    import RiskingOutcome.*
    // Hint: a spoiled apple makes a spoiled basket
    (o1, o2) match
      // format: off
      case (Approved, Approved) => Approved
      case (Approved, FailedFixable) => FailedFixable
      case (Approved, FailedNonFixable) => FailedNonFixable
      case (FailedFixable, Approved) => FailedFixable
      case (FailedFixable, FailedFixable) => FailedFixable
      case (FailedFixable, FailedNonFixable) => FailedNonFixable
      case (FailedNonFixable, Approved) => FailedNonFixable
      case (FailedNonFixable, FailedFixable) => FailedNonFixable
      case (FailedNonFixable, FailedNonFixable) => FailedNonFixable
  // format: on

  def maybeRiskedEntity(applicationWithIndividuals: ApplicationWithIndividuals): Option[RiskedEntity] = applicationWithIndividuals
    .application
    .failures
    .map((failures: List[EntityFailure]) => RiskedEntity(applicationWithIndividuals.application.applicationReference, failures))

  def maybeRiskedIndividuals(applicationWithIndividuals: ApplicationWithIndividuals): Option[Seq[RiskedIndividual]] =
    import cats.implicits.*
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
