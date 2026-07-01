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

import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcome
import play.api.libs.functional.syntax.*
import play.api.libs.json.*

final case class OverallStatus(
  riskingOutcome: Option[RiskingOutcome],
  emailsProcessed: Boolean,
  backendNotified: Boolean
)

object OverallStatus:

  given OFormat[OverallStatus] =
    val reads: Reads[OverallStatus] =
      (
        (__ \ "riskingOutcome").readNullable[RiskingOutcome] and
          (__ \ "emailsProcessed").read[Boolean] and
          (__ \ "backendNotified").readNullable[Boolean].map(_.getOrElse(false))
      )(OverallStatus.apply)
    val writes: OWrites[OverallStatus] = Json.writes[OverallStatus]
    OFormat(reads, writes)
