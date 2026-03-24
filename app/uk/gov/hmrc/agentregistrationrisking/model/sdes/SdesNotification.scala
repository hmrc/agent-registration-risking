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

import play.api.libs.json.*

/** Possible responses from the SDES file service
  */

sealed trait SdesNotification

final case class FileReady(
  filename: String,
  correlationID: String,
  availableUntil: String,
  dateTime: String
)
extends SdesNotification

final case class FileReceived(
  filename: String,
  correlationID: String,
  dateTime: String
)
extends SdesNotification

final case class FileProcessed(
  filename: String,
  correlationID: String,
  dateTime: String
)
extends SdesNotification

final case class FileProcessingFailure(
  filename: String,
  correlationID: String,
  dateTime: String,
  failureReason: String,
  actionRequired: String
)
extends SdesNotification

object SdesNotification:

  given Reads[SdesNotification] =
    given JsonConfiguration = JsonConfiguration(
      discriminator = "notification",
      typeNaming = JsonNaming { fullName =>
        fullName.split('.').last
      }
    )

    given Reads[FileReady] = Json.reads[FileReady]
    given Reads[FileReceived] = Json.reads[FileReceived]
    given Reads[FileProcessed] = Json.reads[FileProcessed]
    given Reads[FileProcessingFailure] = Json.reads[FileProcessingFailure]

    Json.reads[SdesNotification]
