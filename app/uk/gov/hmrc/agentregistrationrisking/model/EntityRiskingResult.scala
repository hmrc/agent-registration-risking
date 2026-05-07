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
import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure

import java.time.Instant

/** The Minerva risking outcome for an application/entity: the failure list and the moment we received it. Both arrive together — they cannot be set
  * independently.
  */
final case class EntityRiskingResult(
  failures: List[EntityFailure],
  receivedAt: Instant
)

object EntityRiskingResult:
  given OFormat[EntityRiskingResult] = Json.format[EntityRiskingResult]
