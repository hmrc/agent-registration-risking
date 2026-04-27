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
import uk.gov.hmrc.agentregistrationrisking.testsupport.UnitSpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.*

class RiskingResponseSpec
extends UnitSpec:

  "risking response reader should parse a JSON array consisting of pass records correctly" in {
    val testRecordArray: JsValue = passRecordArrayFile

    val result: List[RiskingResultRaw] = testRecordArray.validate[List[RiskingResultRaw]].get
    val expected = List(passRecord1, passRecord2)

    result shouldBe expected
  }

  "risking response reader should parse a JSON array consisting of failure records correctly" in {
    val testRecordArray: JsValue = failRecordArrayFile

    val result: List[RiskingResultRaw] = testRecordArray.validate[List[RiskingResultRaw]].get
    val expected = List(
      failRecord1,
      failRecord2,
      failRecord3
    )

    result shouldBe expected
  }
