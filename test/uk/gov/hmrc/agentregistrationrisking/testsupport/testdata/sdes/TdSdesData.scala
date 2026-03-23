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

  val notificationFileName: String = "testFileName.zip"
  val correlationId: String = "testCorrelationId"
  val sdesAvailableUntil: String = "01/02/2024"
  val notificationDate: String = "01/02/2024"
  val failureReason: String = "Virus Detected"
  val failureAction: String = "address-failure-then-retry"

  val fileReadyNotification: JsObject = Json.obj(
    "notification" -> "FileReady",
    "filename" -> notificationFileName,
    "checksumAlgorithm" -> "md5",
    "checksum" -> "123456",
    "correlationID" -> correlationId,
    "availableUntil" -> sdesAvailableUntil,
    "dateTime" -> notificationDate
  )

  val fileReceivedNotification: JsObject = Json.obj(
    "notification" -> "FileReceived",
    "filename" -> notificationFileName,
    "checksumAlgorithm" -> "md5",
    "checksum" -> "123456",
    "correlationID" -> correlationId,
    "dateTime" -> notificationDate,
    "properties" -> Json.arr(
      Json.obj("property 1" -> "value 1", "property 2" -> "value 2")
    )
  )

  val fileProcessedNotification: JsObject = Json.obj(
    "notification" -> "FileReceived",
    "filename" -> notificationFileName,
    "checksumAlgorithm" -> "md5",
    "checksum" -> "123456",
    "correlationID" -> correlationId,
    "dateTime" -> notificationDate,
    "properties" -> Json.arr(
      Json.obj("property 1" -> "value 1", "property 2" -> "value 2")
    )
  )

  val fileProcessingFailureNotification: JsObject = Json.obj(
    "notification" -> "FileProcessingFailure",
    "filename" -> notificationFileName,
    "checksumAlgorithm" -> "md5",
    "checksum" -> "123456",
    "correlationID" -> correlationId,
    "dateTime" -> notificationDate,
    "failureReason" -> failureReason,
    "actionRequired" -> failureAction
  )
