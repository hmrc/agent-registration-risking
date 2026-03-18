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

package uk.gov.hmrc.agentregistrationrisking.model.sdes

import play.api.libs.json.JsValue
import uk.gov.hmrc.agentregistrationrisking.testsupport.UnitSpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.*

class SDESNotificationSpec
extends UnitSpec:

  "reader parses a file upload notification correctly" in:
    val testJsonValue: JsValue = testFileReadyNotification
    val result = testJsonValue.validate[SDESNotification].get

    result shouldBe FileReady(
      testFileName,
      testCorrelationId,
      testSdesAvailableUntil,
      testNotificationDate
    )

  "reader parses a file received notification correctly" in:
    val testJsonValue: JsValue = testFileReceivedNotification
    val result = testJsonValue.validate[SDESNotification].get

    result shouldBe FileReceived(
      testFileName,
      testCorrelationId,
      testNotificationDate
    )

  "reader parses a file processed notification correctly" in:
    val testJsonValue: JsValue = testFileProcessedNotification
    val result = testJsonValue.validate[SDESNotification].get

    result shouldBe FileReceived(
      testFileName,
      testCorrelationId,
      testNotificationDate
    )

  "reader parses a file processing failure notification correctly" in:
    val testJsonValue: JsValue = testFileProcessingFailureNotification
    val result = testJsonValue.validate[SDESNotification].get

    result shouldBe FileProcessingFailure(
      testFileName,
      testCorrelationId,
      testNotificationDate,
      testFailureReason,
      testFailureAction
    )
