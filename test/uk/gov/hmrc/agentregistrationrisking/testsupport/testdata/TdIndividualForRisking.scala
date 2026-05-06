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

import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking

import java.time.Instant
import java.time.temporal.ChronoUnit

object TdIndividualForRisking:
  def make(
    instant: Instant,
    applicationReference: ApplicationReference,
    individualProvidedDetails: IndividualProvidedDetails
  ): TdIndividualForRisking =
    def instantParam: Instant = instant
    def applicationReferenceParam: ApplicationReference = applicationReference
    def individualProvidedDetailsParam: IndividualProvidedDetails = individualProvidedDetails

    new TdIndividualForRisking:
      override def instant: Instant = instantParam
      override def personReference: PersonReference = individualProvidedDetails.personReference
      override def applicationReference: ApplicationReference = applicationReferenceParam
      override def individualProvidedDetails: IndividualProvidedDetails = individualProvidedDetailsParam

trait TdIndividualForRisking:

  def instant: Instant
  def personReference: PersonReference
  def applicationReference: ApplicationReference
  def individualProvidedDetails: IndividualProvidedDetails

  def readyForSubmission: IndividualForRisking = IndividualForRisking(
    personReference = personReference,
    applicationReference = applicationReference,
    individualProvidedDetails = individualProvidedDetails,
    createdAt = instant,
    lastUpdatedAt = instant,
    failures = None,
    riskingCompletedDate = None
  )

  // nothing changes from data perspective
  def submittedForRisking: IndividualForRisking = readyForSubmission.copy()

  object receivedRiskingResults:

    def approved: IndividualForRisking = submittedForRisking.copy(
      failures = Some(List.empty),
      riskingCompletedDate = Some(instant.minus(2, ChronoUnit.DAYS))
    )

    def failedFixable: IndividualForRisking = submittedForRisking.copy(
      failures = Some(List(
        TdFailures.individualFailures.fixable1,
        TdFailures.individualFailures.fixable2
      )),
      riskingCompletedDate = Some(instant.minus(2, ChronoUnit.DAYS))
    )

    def applicationFailedNonFixable: IndividualForRisking = submittedForRisking.copy(
      failures = Some(List(
        TdFailures.individualFailures.fixable2,
        TdFailures.individualFailures.nonFixable2
      )),
      riskingCompletedDate = Some(instant.minus(2, ChronoUnit.DAYS))
    )
