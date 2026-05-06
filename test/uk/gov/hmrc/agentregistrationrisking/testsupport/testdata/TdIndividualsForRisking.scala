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

import uk.gov.hmrc.agentregistration.shared
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference

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
