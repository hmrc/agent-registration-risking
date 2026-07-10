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

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcome
import uk.gov.hmrc.agentregistrationrisking.testsupport.UnitSpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdInstant

import java.time.Instant

class OverallStatusSpec
extends UnitSpec:

  "serialize and deserialize" in:

    val emailsSentAt: Instant = TdInstant.instant
    val overallStatus: OverallStatus = OverallStatus(
      riskingOutcome = Some(RiskingOutcome.Approved),
      emailsProcessed = true,
      backendNotified = false,
      emailsSentAt = Some(emailsSentAt)
    )
    val json: JsValue = Json.parse(
      // language=JSON
      s"""{
         |  "riskingOutcome":"Approved",
         |  "emailsProcessed":true,
         |  "backendNotified":false,
         |  "emailsSentAt":"$emailsSentAt"
         |}""".stripMargin
    )
    Json.toJson[OverallStatus](overallStatus) shouldBe json
    json.as[OverallStatus] shouldBe overallStatus

  "deserialize backward compatible - missing backendNotified and emailSentAt fields default to false and None" in:

    val overallStatus: OverallStatus = OverallStatus(
      riskingOutcome = Some(RiskingOutcome.Approved),
      emailsProcessed = true,
      backendNotified = false, // defaults to false
      emailsSentAt = None
    )
    val json: JsValue = Json.parse(
      // language=JSON
      """{
        |  "riskingOutcome":"Approved",
        |  "emailsProcessed":true
        |}""".stripMargin
    )
    json.as[OverallStatus] shouldBe overallStatus

  "deserialize backward compatible - missing only backendNotified defaults to false while emailSentAt is preserved" in:

    val emailsSentAt: Instant = TdInstant.instant
    val overallStatus: OverallStatus = OverallStatus(
      riskingOutcome = Some(RiskingOutcome.Approved),
      emailsProcessed = true,
      backendNotified = false,
      emailsSentAt = Some(emailsSentAt)
    )
    val json: JsValue = Json.parse(
      // language=JSON
      s"""{
         |  "riskingOutcome":"Approved",
         |  "emailsProcessed":true,
         |  "emailsSentAt":"$emailsSentAt"
         |}""".stripMargin
    )
    json.as[OverallStatus] shouldBe overallStatus

  "deserialize backward compatible - missing only emailSentAt defaults to None while backendNotified is preserved" in:

    val overallStatus: OverallStatus = OverallStatus(
      riskingOutcome = Some(RiskingOutcome.Approved),
      emailsProcessed = true,
      backendNotified = true,
      emailsSentAt = None
    )
    val json: JsValue = Json.parse(
      // language=JSON
      """{
        |  "riskingOutcome":"Approved",
        |  "emailsProcessed":true,
        |  "backendNotified":true
        |}""".stripMargin
    )
    json.as[OverallStatus] shouldBe overallStatus
