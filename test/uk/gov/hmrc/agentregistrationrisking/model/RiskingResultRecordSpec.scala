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
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistrationrisking.testsupport.UnitSpec

class RiskingResultRecordSpec
extends UnitSpec:

  "deserialize entity record" in:
    val record = RiskingResultRecord(
      recordType = RecordType.Entity,
      applicationReference = Some(ApplicationReference("XKXP9HEZB")),
      failures = Some(List(Failure(
        reasonCode = "3.1",
        reasonDescription = "Entity claims AMLS with HMRC but their registration number cannot be found in HMRC's AMLS register",
        checkId = "3",
        checkDescription = "AMLS"
      ))),
      personReference = None
    )
    val json: JsValue = Json.parse(
      // language=JSON
      """{
        |  "recordType": "Entity",
        |  "applicationReference": "XKXP9HEZB",
        |  "failures": [
        |    {
        |      "reasonCode": "3.1",
        |      "reasonDescription": "Entity claims AMLS with HMRC but their registration number cannot be found in HMRC's AMLS register",
        |      "checkId": "3",
        |      "checkDescription": "AMLS"
        |    }
        |  ]
        |}""".stripMargin
    )
    json.as[RiskingResultRecord] shouldBe record

  "deserialize individual record" in:
    val record = RiskingResultRecord(
      recordType = RecordType.Individual,
      applicationReference = None,
      failures = Some(List(Failure(
        reasonCode = "8.1",
        reasonDescription = "Measure - Published Tax Avoidance promoters, enablers and suppliers",
        checkId = "8",
        checkDescription = "Anti-avoidance measures or penalties"
      ))),
      personReference = Some(PersonReference("JJFCXYTM4"))
    )
    val json: JsValue = Json.parse(
      // language=JSON
      """{
        |  "recordType": "Individual",
        |  "personReference": "JJFCXYTM4",
        |  "failures": [
        |    {
        |      "reasonCode": "8.1",
        |      "reasonDescription": "Measure - Published Tax Avoidance promoters, enablers and suppliers",
        |      "checkId": "8",
        |      "checkDescription": "Anti-avoidance measures or penalties"
        |    }
        |  ]
        |}""".stripMargin
    )
    json.as[RiskingResultRecord] shouldBe record
