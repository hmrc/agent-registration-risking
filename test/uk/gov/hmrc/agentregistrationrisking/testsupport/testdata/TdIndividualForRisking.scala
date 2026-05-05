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

object TdIndividualsForRisking:

  def make(
    instantParam: Instant,
    personReferencePrefixParam: String,
    applicationReferenceParam: ApplicationReference
  ): TdIndividualsForRisking =
    new TdIndividualsForRisking
      with TdRiskingBase:
      override def instant: Instant = instantParam
      override def personReferencePrefix: String = personReferencePrefixParam
      override def applicationReference: ApplicationReference = applicationReferenceParam

trait TdIndividualsForRisking {
  dependencies: TdRiskingBase =>

  def instant: Instant
  def applicationReference: ApplicationReference
  def personReferencePrefix: String

  def tdIndividualForRisking1: TdIndividualForRisking = TdIndividualForRisking.make(
    instant = dependencies.instant,
    applicationReference = applicationReference,
    individualProvidedDetails =
      TdIndividualProvidedDetailsFactory
        .make(
          applicationReference = applicationReference,
          personReference = PersonReference(s"${personReferencePrefix}01")
        )
        .providedDetails
        .afterFinished
  )

  def tdIndividualForRisking2: TdIndividualForRisking = TdIndividualForRisking.make(
    instant = dependencies.instant,
    applicationReference = applicationReference,
    individualProvidedDetails =
      TdIndividualProvidedDetailsFactory
        .make(
          applicationReference = applicationReference,
          personReference = PersonReference(s"${personReferencePrefix}02")
        )
        .providedDetails
        .afterFinished
  )

}

object TdIndividualForRisking:
  def make(
    instant: Instant,
    applicationReference: ApplicationReference,
    individualProvidedDetails: IndividualProvidedDetails
  ): TdIndividualForRisking =
    new TdIndividualForRisking:
      val instantParam: Instant = instant
      val applicationReferenceParam: ApplicationReference = applicationReference
      val individualProvidedDetailsParam: IndividualProvidedDetails = individualProvidedDetails
      def instant: Instant = instantParam
      def personReference: PersonReference = individualProvidedDetails.personReference
      def applicationReference: ApplicationReference = applicationReferenceParam
      def individualProvidedDetails: IndividualProvidedDetails = individualProvidedDetailsParam

trait TdIndividualForRisking:

  def instant: Instant
  def personReference: PersonReference
  def applicationReference: ApplicationReference
  def individualProvidedDetails: IndividualProvidedDetails

  def submitted: IndividualForRisking = IndividualForRisking(
    personReference = personReference,
    applicationReference = applicationReference,
    individualProvidedDetails = individualProvidedDetails,
    createdAt = instant,
    lastUpdatedAt = instant,
    failures = None
  )

  // nothing changes from data perspective
  def sent: IndividualForRisking = submitted.copy()

  object receivedRiskingResults:

    def approved: IndividualForRisking = sent.copy(
      failures = Some(List.empty)
    )

    def failedFixable: IndividualForRisking = sent.copy(
      failures = Some(List(
        TdFailures.individualFailures.fixable1,
        TdFailures.individualFailures.fixable2
      ))
    )

    def applicationFailedNonFixable: IndividualForRisking = sent.copy(
      failures = Some(List(
        TdFailures.individualFailures.fixable2,
        TdFailures.individualFailures.nonFixable2
      ))
    )
