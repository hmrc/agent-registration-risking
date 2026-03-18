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

package uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.sdes

import play.api.libs.json.JsObject
import play.api.libs.json.Json

trait TdSdesData:

  val testFileName = "testFileName.zip"
  val testCorrelationId = "testCorrelationId"
  val testSdesAvailableUntil = "01/02/2024"
  val testNotificationDate = "01/02/2024"
  val testFailureReason = "Virus Detected"
  val testFailureAction = "address-failure-then-retry"

  val testFileReadyNotification: JsObject = Json.obj(
    "notification" -> "FileReady",
    "filename" -> "testFileName.zip",
    "checksumAlgorithm" -> "md5",
    "checksum" -> "123456",
    "correlationID" -> "testCorrelationId",
    "availableUntil" -> "01/02/2024",
    "dateTime" -> "01/02/2024"
  )

  val testFileReceivedNotification: JsObject = Json.obj(
    "notification" -> "FileReceived",
    "filename" -> "testFileName.zip",
    "checksumAlgorithm" -> "md5",
    "checksum" -> "123456",
    "correlationID" -> "testCorrelationId",
    "dateTime" -> "01/02/2024",
    "properties" -> Json.arr(
      Json.obj("property 1" -> "value 1", "property 2" -> "value 2")
    )
  )

  val testFileProcessedNotification: JsObject = Json.obj(
    "notification" -> "FileReceived",
    "filename" -> "testFileName.zip",
    "checksumAlgorithm" -> "md5",
    "checksum" -> "123456",
    "correlationID" -> "testCorrelationId",
    "dateTime" -> "01/02/2024",
    "properties" -> Json.arr(
      Json.obj("property 1" -> "value 1", "property 2" -> "value 2")
    )
  )

  val testFileProcessingFailureNotification: JsObject = Json.obj(
    "notification" -> "FileProcessingFailure",
    "filename" -> "testFileName.zip",
    "checksumAlgorithm" -> "md5",
    "checksum" -> "123456",
    "correlationID" -> "testCorrelationId",
    "dateTime" -> "01/02/2024",
    "failureReason" -> "Virus Detected",
    "actionRequired" -> "address-failure-then-retry"
  )
