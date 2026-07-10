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

import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicationData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.SubmitForRiskingRequest
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.CompletedRisking
import uk.gov.hmrc.agentregistrationrisking.model.CompletedRiskingId
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFile
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileName

import java.time.Instant

trait TdRisking:

  def seed: String
  def personReferencePrefix: String = s"PERSON_REF_$seed"

  def applicationData: ApplicationData = TdApplicationData.make(seed)
  def instant: Instant
  def riskingFileName: RiskingFileName

  def riskingFile: RiskingFile = RiskingFile(
    riskingFileName = riskingFileName,
    uploadedAt = instant
  )

  def tdApplicationForRisking: TdApplicationForRisking = TdApplicationForRisking.make(
    instant = instant,
    riskingFileName = riskingFileName,
    applicationData = applicationData
  )

  def tdIndividualsForRisking: TdIndividualsForRisking = TdIndividualsForRisking.make(
    instantParam = instant,
    seed,
    applicationReferenceParam = applicationData.applicationReference
  )

  def submitForRiskingRequest: SubmitForRiskingRequest = SubmitForRiskingRequest(
    applicationData = applicationData,
    individuals = List(
      tdIndividualsForRisking.tdIndividualForRisking1.readyForSubmission.individualData,
      tdIndividualsForRisking.tdIndividualForRisking2.readyForSubmission.individualData
    ),
    isResubmission = false
  )

object TdRisking:

  def make(
    seed: String,
    instant: Instant = TdInstant.instant
  ): TdRisking =

    val instantP: Instant = instant
    val seedP = seed

    new TdRisking:
      override def seed: String = seedP
      override def instant: Instant = instantP
      override def riskingFileName: RiskingFileName = RiskingFileName.make(instantP)
