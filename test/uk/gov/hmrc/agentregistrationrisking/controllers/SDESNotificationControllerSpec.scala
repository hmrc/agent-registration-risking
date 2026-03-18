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

package uk.gov.hmrc.agentregistrationrisking.controllers

import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import play.api.mvc.Call
import uk.gov.hmrc.agentregistrationrisking.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.*
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.AuthStubs

class SDESNotificationControllerSpec
extends ControllerSpec:

  val path: String = s"/agent-registration-risking/receive-sdes-notifications"

  "SDES Notification controller should have the correct route" in:
    val call: Call = routes.SDESNotificationController.processSDESNotification
    call shouldBe Call(
      method = "POST",
      url = path
    )

  "receiveNotification should handle the File Ready Notification correctly" in:
    val SDESNotification: JsObject = testFileReadyNotification
    AuthStubs.stubAuthorise()

    val response: WSResponse =
      wsClient
        .url(s"$baseUrl/agent-registration-risking/receive-sdes-notifications")
        .withHttpHeaders(
          "Content-Type" -> "application/json",
          "Authorization" -> "test_token"
        )
        .post(SDESNotification)
        .futureValue

    response.status shouldBe Status.OK
    response.body[String] === ""

  "receiveNotifications should handle the File Received Notification correctly" in:
    val SDESNotification = testFileReceivedNotification
    AuthStubs.stubAuthorise()

    val response: WSResponse =
      wsClient
        .url(s"$baseUrl/agent-registration-risking/receive-sdes-notifications")
        .withHttpHeaders(
          "Content-Type" -> "application/json",
          "Authorization" -> "test_token"
        )
        .post(SDESNotification)
        .futureValue
    response.status shouldBe Status.OK
    response.body[String] === ""

  "receiveNotifications should handle the File Processing Failed Notification correctly" in:
    val SDESNotification = testFileProcessingFailureNotification
    AuthStubs.stubAuthorise()

    val response: WSResponse =
      wsClient
        .url(s"$baseUrl/agent-registration-risking/receive-sdes-notifications")
        .withHttpHeaders(
          "Content-Type" -> "application/json",
          "Authorization" -> "test_token"
        )
        .post(SDESNotification)
        .futureValue
    response.status shouldBe Status.OK
    response.body[String] === ""

  "receiveNotification should handle the File Processed Notification correctly" in:
    val SDESNotification = testFileProcessedNotification
    AuthStubs.stubAuthorise()

    val response: WSResponse =
      wsClient
        .url(s"$baseUrl/agent-registration-risking/receive-sdes-notifications")
        .withHttpHeaders(
          "Content-Type" -> "application/json",
          "Authorization" -> "test_token"
        )
        .post(SDESNotification)
        .futureValue

    response.status shouldBe Status.OK
    response.body[String] === ""

  "should handle malformed JSON correctly" in:
    val SDESNotification = JsString("{Test Bad Json")
    AuthStubs.stubAuthorise()

    val response: WSResponse =
      wsClient
        .url(s"$baseUrl/agent-registration-risking/receive-sdes-notifications")
        .withHttpHeaders(
          "Content-Type" -> "application/json",
          "Authorization" -> "test_token"
        )
        .post(SDESNotification)
        .futureValue

    response.status shouldBe Status.BAD_REQUEST
    response.body[String] === ""
