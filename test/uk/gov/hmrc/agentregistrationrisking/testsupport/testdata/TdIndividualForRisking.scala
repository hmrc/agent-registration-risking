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

import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference

import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.IndividualData
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualRiskingResult

import java.time.Instant
import java.time.temporal.ChronoUnit

object TdIndividualForRisking:
  def make(
    instant: Instant,
    applicationReference: ApplicationReference,
    individualData: IndividualData
  ): TdIndividualForRisking =
    def instantParam: Instant = instant
    def applicationReferenceParam: ApplicationReference = applicationReference
    def individualDataParam: IndividualData = individualData

    new TdIndividualForRisking:
      override def instant: Instant = instantParam
      override def personReference: PersonReference = individualData.personReference
      override def applicationReference: ApplicationReference = applicationReferenceParam
      override def individualData: IndividualData = individualDataParam

trait TdIndividualForRisking:

  def instant: Instant
  def personReference: PersonReference
  def applicationReference: ApplicationReference
  def individualData: IndividualData

  def readyForSubmission: IndividualForRisking = IndividualForRisking(
    personReference = personReference,
    applicationReference = applicationReference,
    individualData = individualData,
    createdAt = instant,
    lastUpdatedAt = instant,
    individualRiskingResult = None,
    isEmailSent = false,
    isResubmission = false
  )

  // nothing changes from data perspective
  def submittedForRisking: IndividualForRisking = readyForSubmission.copy()

  object receivedRiskingResults:

    def approved: IndividualForRisking = submittedForRisking.copy(
      individualRiskingResult = Some(IndividualRiskingResult(
        failures = List.empty,
        receivedAt = instant.minus(2, ChronoUnit.DAYS)
      ))
    )

    def failedFixable: IndividualForRisking = submittedForRisking.copy(
      individualRiskingResult = Some(IndividualRiskingResult(
        failures = List(
          TdFailures.individualFailures.fixable1,
          TdFailures.individualFailures.fixable2
        ),
        receivedAt = instant.minus(2, ChronoUnit.DAYS)
      ))
    )

    def failedNonFixable: IndividualForRisking = submittedForRisking.copy(
      individualRiskingResult = Some(IndividualRiskingResult(
        failures = List(
          TdFailures.individualFailures.fixable2,
          TdFailures.individualFailures.nonFixable2
        ),
        receivedAt = instant.minus(2, ChronoUnit.DAYS)
      ))
    )

    def failedNonFixableEmailSent: IndividualForRisking = failedNonFixable.copy(isEmailSent = true)

    def failedFixableEmailSent: IndividualForRisking = failedFixable.copy(isEmailSent = true)
