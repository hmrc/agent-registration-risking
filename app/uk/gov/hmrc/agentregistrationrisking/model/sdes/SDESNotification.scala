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

import play.api.libs.json.JsError
import play.api.libs.json.JsPath
import play.api.libs.json.Json
import play.api.libs.json.Reads

/** Possible responses from the SDES file service
  */

type SDESNotification = FileReady | FileReceived | FileProcessed | FileProcessingFailure

final case class FileReady(
  filename: String,
  correlationID: String,
  availableUntil: String,
  dateTime: String
)

final case class FileReceived(
  filename: String,
  correlationID: String,
  dateTime: String
)

final case class FileProcessed(
  filename: String,
  correlationID: String,
  dateTime: String
)

final case class FileProcessingFailure(
  filename: String,
  correlationID: String,
  dateTime: String,
  failureReason: String,
  actionRequired: String
)

given Reads[FileReady] = Json.reads[FileReady]
given Reads[FileReceived] = Json.reads[FileReceived]
given Reads[FileProcessed] = Json.reads[FileProcessed]
given Reads[FileProcessingFailure] = Json.reads[FileProcessingFailure]

given Reads[SDESNotification] = Reads: json =>
  (json \ "notification").validate[String].flatMap:
    case "FileReady" => json.validate[FileReady]
    case "FileReceived" => json.validate[FileReceived]
    case "FileProcessed" => json.validate[FileProcessed]
    case "FileProcessingFailure" => json.validate[FileProcessingFailure]
    case _ => JsError(s"Received Malformed SDESNotification")
