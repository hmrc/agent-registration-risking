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

package uk.gov.hmrc.agentregistration.shared.risking

enum RiskingOutcome:

  case FailedNonFixable
  case FailedFixable
  case Approved

object RiskingOutcome:

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
      case (Approved,         Approved)         => Approved
      case (Approved,         FailedFixable)    => FailedFixable
      case (Approved,         FailedNonFixable) => FailedNonFixable
      case (FailedFixable,    Approved)         => FailedFixable
      case (FailedFixable,    FailedFixable)    => FailedFixable
      case (FailedFixable,    FailedNonFixable) => FailedNonFixable
      case (FailedNonFixable, Approved)         => FailedNonFixable
      case (FailedNonFixable, FailedFixable)    => FailedNonFixable
      case (FailedNonFixable, FailedNonFixable) => FailedNonFixable
      // format: on
