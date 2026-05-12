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

package uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.partiallyrisked

import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRisking
import uk.gov.hmrc.agentregistration.shared.risking.RiskingProgress

case object approved_failedNonFixable_submitted
extends TdApplicationWithIndividuals:

  override val tdRisking: TdRisking = TdRisking.make(this.toString)
  override val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.receivedRiskingResults.beforeApproved
  override val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.failedNonFixable
  override val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.submittedForRisking

  override def riskingProgressForApplicant: RiskingProgress = RiskingProgress.SubmittedForRisking
