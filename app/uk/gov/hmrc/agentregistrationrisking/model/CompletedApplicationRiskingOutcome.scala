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

package uk.gov.hmrc.agentregistrationrisking.model

import uk.gov.hmrc.agentregistration.shared.risking.ApplicationForRiskingStatus
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationForRiskingStatus.RiskingCompletedStatus
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationReference
final case class CompletedApplicationRiskingOutcome(
  applicationReference: ApplicationReference,
  entityStatus: RiskingCompletedStatus,
  individualStatuses: List[RiskingCompletedStatus]
):

  def applicationStatus: RiskingCompletedStatus =
    val allStatuses = entityStatus :: individualStatuses
    if allStatuses.contains(ApplicationForRiskingStatus.FailedNonFixable) then
      ApplicationForRiskingStatus.FailedNonFixable
    else if allStatuses.contains(ApplicationForRiskingStatus.FailedFixable) then
      ApplicationForRiskingStatus.FailedFixable
    else
      ApplicationForRiskingStatus.Approved
