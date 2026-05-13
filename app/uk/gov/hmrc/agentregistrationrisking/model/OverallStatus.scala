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

import play.api.libs.json.Json
import play.api.libs.json.OFormat

/** Represents the overall status for the [[ApplicationForRisking]] class.
  *
  * @param riskingOutcome
  *   the outcome of the risking process
  * @param emailsProcessed
  *   whether all required emails have been sent for this application after computed riskingOutcome
  */
final case class OverallStatus(
  riskingOutcome: Option[RiskingOutcome],
  emailsProcessed: Boolean
)

object OverallStatus:
  given OFormat[OverallStatus] = Json.format[OverallStatus]
