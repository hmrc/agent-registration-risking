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

package uk.gov.hmrc.agentregistrationrisking.testsupport.testdata

import org.scalatest.matchers.should.Matchers.shouldBe
import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistrationrisking.model.*
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcome
import uk.gov.hmrc.agentregistrationrisking.testsupport.RichMatchers.*

import java.time.Instant

trait TdCompletedRisking { self: TdApplicationWithIndividuals =>

  def completedRisking: CompletedRisking = TdCompletedRisking.makeCompletedRisking(
    completedRiskingId = CompletedRiskingId(s"CompletedRiskingId_${self.tdRisking.seed}"),
    completedAt = self.tdRisking.instant,
    riskingFile = self.tdRisking.riskingFile,
    application = self.applicationWithIndividuals.application,
    individuals = self.applicationWithIndividuals.individuals
  )
}

object TdCompletedRisking:

  def makeCompletedRisking(
    tdRisking: TdRisking,
    completedRiskingId: CompletedRiskingId = CompletedRiskingId("CR_123"),
    completedAt: Instant = Instant.parse("2059-12-25T16:33:51Z")
  ): CompletedRisking = makeCompletedRisking(
    completedRiskingId = completedRiskingId,
    completedAt = completedAt,
    riskingFile = tdRisking.riskingFile,
    application =
      tdRisking
        .tdApplicationForRisking
        .receivedRiskingResults
        .approvedAfterEmailsProcessed,
    individuals = Seq(
      tdRisking
        .tdIndividualsForRisking
        .tdIndividualForRisking1
        .receivedRiskingResults
        .approved
    )
  )

  /** Creates a CompletedRisking test data object from a TdRisking instance.
    *
    * This method performs data integrity checks to ensure the application and individuals have valid risking results and outcomes.
    * @throws org.scalatest.exceptions.TestFailedException
    *   if data integrity checks fail
    */
  def makeCompletedRisking(
    completedRiskingId: CompletedRiskingId,
    completedAt: Instant,
    riskingFile: RiskingFile,
    application: ApplicationForRisking,
    individuals: Seq[IndividualForRisking]
  ): CompletedRisking =

    withClue("data integrity check"):
      application.overallStatus.riskingOutcome.isDefined shouldBe true
      application.entityRiskingResult.isDefined shouldBe true
      individuals.foreach: individual =>
        individual.individualRiskingResult.isDefined shouldBe true

      application.overallStatus.riskingOutcome.value match
        case RiskingOutcome.Approved =>
          application.entityRiskingResult.value.failures shouldBe Seq.empty
          individuals.foreach: individual =>
            individual.individualRiskingResult.value.failures shouldBe Seq.empty

        case RiskingOutcome.FailedFixable =>
          application.entityRiskingResult.value.failures.foreach: failure =>
            failure shouldBe a[EntityFailure.Fixable]
          individuals.foreach: individual =>
            individual.individualRiskingResult.value.failures.foreach: failure =>
              failure shouldBe a[IndividualFailure.Fixable]

        case RiskingOutcome.FailedNonFixable =>

          val hasApplicationNonFixableFailure: Boolean = application
            .entityRiskingResult
            .value
            .failures
            .exists:
              case _: EntityFailure.NonFixable => true
              case _: EntityFailure.Fixable => false

          val hasAnyIndividualNonFixableFailure: Boolean = individuals
            .flatMap(
              _.individualRiskingResult.value.failures
            ).exists:
              case _: IndividualFailure.NonFixable => true
              case _: IndividualFailure.Fixable => false

          (hasApplicationNonFixableFailure || hasAnyIndividualNonFixableFailure) shouldBe true

    CompletedRisking(
      _id = completedRiskingId,
      completedAt = completedAt,
      riskingFile = riskingFile,
      application = application,
      individuals = individuals
    )
