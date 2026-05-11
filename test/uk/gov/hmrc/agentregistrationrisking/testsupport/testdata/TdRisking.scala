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

import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.risking.SubmitForRiskingRequest
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileName

import java.time.Instant

trait TdRisking:

  def agentApplication: AgentApplication
  def personReferencePrefix: String
  def instant: Instant
  def riskingFileName: RiskingFileName

  def tdApplicationForRisking: TdApplicationForRisking = TdApplicationForRisking.make(
    instant = instant,
    riskingFileName = riskingFileName,
    agentApplication = agentApplication
  )

  def tdIndividualsForRisking: TdIndividualsForRisking = TdIndividualsForRisking.make(
    instantParam = instant,
    personReferencePrefixParam = personReferencePrefix,
    applicationReferenceParam = agentApplication.applicationReference
  )

  def submitForRiskingRequest: SubmitForRiskingRequest = SubmitForRiskingRequest(
    agentApplication = agentApplication,
    individuals = List(
      tdIndividualsForRisking.tdIndividualForRisking1.readyForSubmission.individualProvidedDetails,
      tdIndividualsForRisking.tdIndividualForRisking2.readyForSubmission.individualProvidedDetails
    )
  )

object TdRisking:

  def make(
    instant: Instant,
    agentApplication: AgentApplication,
    personReferencePrefix: String,
    riskingFileName: RiskingFileName
  ): TdRisking =
    val instantParam: Instant = instant
    val agentApplicationParam: AgentApplication = agentApplication
    val personReferencePrefixParam: String = personReferencePrefix
    val riskingFileNameParam: RiskingFileName = riskingFileName
    new TdRisking:
      override def instant: Instant = instantParam
      override def agentApplication: AgentApplication = agentApplicationParam
      override def personReferencePrefix: String = personReferencePrefixParam
      override def riskingFileName: RiskingFileName = riskingFileNameParam
