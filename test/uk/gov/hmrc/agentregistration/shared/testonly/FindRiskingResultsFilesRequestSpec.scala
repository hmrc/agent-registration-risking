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

package uk.gov.hmrc.agentregistration.shared.testonly

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistrationrisking.testsupport.UnitSpec

class FindRiskingResultsFilesRequestSpec
extends UnitSpec:

  "serialize and deserialize" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{
        |  "applicationReferences": ["XARN0000001"],
        |  "personReferences": ["PERS0000001", "PERS0000002"]
        |}""".stripMargin
    )
    val findRiskingResultsFilesRequest: FindRiskingResultsFilesRequest = FindRiskingResultsFilesRequest(
      applicationReferences = List(ApplicationReference("XARN0000001")),
      personReferences = List(PersonReference("PERS0000001"), PersonReference("PERS0000002"))
    )

    Json.toJson(findRiskingResultsFilesRequest) shouldBe json
    json.as[FindRiskingResultsFilesRequest] shouldBe findRiskingResultsFilesRequest
