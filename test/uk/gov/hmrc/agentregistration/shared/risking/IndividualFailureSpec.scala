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

package uk.gov.hmrc.agentregistration.shared.risking

import play.api.libs.json.*
import uk.gov.hmrc.agentregistrationrisking.testsupport.UnitSpec

class IndividualFailureSpec
extends UnitSpec:

  private def roundTrip(
    failure: IndividualFailure,
    expectedJson: JsObject
  ): Unit =
    Json.toJson[IndividualFailure](failure) shouldBe expectedJson
    expectedJson.as[IndividualFailure] shouldBe failure

  "IndividualFailure" - {
    "serialises and deserialises to/from JSON" - {
      "Check 4: Overdue returns" - {
        "4.1 one or more overdue SA returns" in:
          roundTrip(IndividualFailure._4._1, Json.obj("type" -> "_4._1"))

        "4.3 one or more overdue VAT returns" in:
          roundTrip(IndividualFailure._4._3, Json.obj("type" -> "_4._3"))

        "4.4 one or more overdue PAYE returns" in:
          roundTrip(IndividualFailure._4._4, Json.obj("type" -> "_4._4"))
      }

      "Check 5: Overdue liabilities" - {
        "5.1 one or more overdue SA liabilities" in:
          roundTrip(IndividualFailure._5._1(100.50), Json.obj("type" -> "_5._1", "value" -> 100.50))

        "5.3 one or more overdue VAT liabilities" in:
          roundTrip(IndividualFailure._5._3(200.00), Json.obj("type" -> "_5._3", "value" -> 200.00))

        "5.4 one or more overdue PAYE liabilities" in:
          roundTrip(IndividualFailure._5._4(300.75), Json.obj("type" -> "_5._4", "value" -> 300.75))

        "5.5 one or more overdue civil penalties" in:
          roundTrip(IndividualFailure._5._5(450.99), Json.obj("type" -> "_5._5", "value" -> 450.99))

        "5.6 one or more overdue Stamp Duty liabilities" in:
          roundTrip(IndividualFailure._5._6(1000.00), Json.obj("type" -> "_5._6", "value" -> 1000.00))

        "5.7 one or more overdue Capital Gains Tax liabilities" in:
          roundTrip(IndividualFailure._5._7(5000.50), Json.obj("type" -> "_5._7", "value" -> 5000.50))
      }

      "6 disqualified as a director on Companies House" in:
        roundTrip(IndividualFailure._6, Json.obj("type" -> "_6"))

      "7 insolvent" in:
        roundTrip(IndividualFailure._7, Json.obj("type" -> "_7"))

      "Check 8: Anti-avoidance measures or penalties" - {
        "8.1 published tax avoidance promoters, enablers and suppliers" in:
          roundTrip(IndividualFailure._8._1, Json.obj("type" -> "_8._1"))

        "8.6 enablers penalty within 12 months" in:
          roundTrip(IndividualFailure._8._6, Json.obj("type" -> "_8._6"))

        "8.7 enablers penalty more than 12 months, not paid" in:
          roundTrip(IndividualFailure._8._7, Json.obj("type" -> "_8._7"))
      }

      "9 relevant criminal convictions" in:
        roundTrip(IndividualFailure._9, Json.obj("type" -> "_9"))

      "Check 10: Cannot verify the individual's information" - {
        "10.1 unable to match Name and DOB against references" in:
          roundTrip(IndividualFailure._10._1, Json.obj("type" -> "_10._1"))

        "10.2 unable to match Name and DOB against references and missing SA-UTR" in:
          roundTrip(IndividualFailure._10._2, Json.obj("type" -> "_10._2"))
      }
    }
  }
