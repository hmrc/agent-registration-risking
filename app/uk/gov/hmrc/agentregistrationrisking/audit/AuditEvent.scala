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

package uk.gov.hmrc.agentregistrationrisking.audit

import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.OFormat
import play.api.libs.json.OWrites
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcome
import uk.gov.hmrc.agentregistration.shared.util.JsonFormatsFactory

sealed trait AuditEvent:

  val applicationReference: ApplicationReference
  val auditType: String = this.getClass.getSimpleName


final case class RiskingResponseEntity(
                                        applicationReference: ApplicationReference,
                                        riskingOutcome: AuditOutcome,
                                        failures: Option[List[FailureDetail]]
)
extends AuditEvent

object RiskingResponseEntity:
  given OWrites[RiskingResponseEntity] = Json.writes[RiskingResponseEntity]

final case class RiskingResponseIndividual(
                                            applicationReference: ApplicationReference,
                                            personReference: PersonReference,
                                            riskingOutcome: AuditOutcome,
                                            failures: Option[List[FailureDetail]]
)
extends AuditEvent

object RiskingResponseIndividual:
  given OWrites[RiskingResponseIndividual] = Json.writes[RiskingResponseIndividual]

final case class FailureDetail(
  reasonCode: String,
  reasonDescription: String
)

object FailureDetail:
  given OFormat[FailureDetail] = Json.format[FailureDetail]

enum AuditOutcome:
  case Success
  case NonFixableFailure
  case FixableFailure

object AuditOutcome:

  given Format[AuditOutcome] = JsonFormatsFactory.makeEnumFormat

  def fromRiskingOutcome(outcome: RiskingOutcome): AuditOutcome = outcome match
    case RiskingOutcome.Approved         => Success
    case RiskingOutcome.FailedNonFixable => NonFixableFailure
    case RiskingOutcome.FailedFixable    => FixableFailure
