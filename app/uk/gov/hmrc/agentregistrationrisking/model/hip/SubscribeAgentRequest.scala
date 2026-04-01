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

package uk.gov.hmrc.agentregistrationrisking.model.hip

import play.api.libs.json.Json
import play.api.libs.json.OFormat

// Payload for calls to subscribe a new agent using HIP API Agent Subscription
// https://admin.tax.service.gov.uk/integration-hub/apis/view-specification/ed3bdeb8-6db7-4c20-91c9-8b144aa1736b/test#tag/Agent-Subscription

final case class SubscribeAgentRequest(
  name: String,
  addr1: String,
  addr2: String,
  addr3: Option[String],
  addr4: Option[String],
  postcode: Option[String],
  country: String,
  phone: Option[String],
  email: String,
  supervisoryBody: Option[String],
  membershipNumber: Option[String],
  evidenceObjectReference: Option[String],
  updateDetailsStatus: String,
  amlSupervisionUpdateStatus: String,
  directorPartnerUpdateStatus: String,
  acceptNewTermsStatus: String,
  reriskStatus: String
)

object SubscribeAgentRequest:
  given format: OFormat[SubscribeAgentRequest] = Json.format[SubscribeAgentRequest]
