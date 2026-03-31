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

import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.Writes
import uk.gov.hmrc.agentregistration.shared.util.JsonFormatsFactory

/** Notification sent to SDES when a file is ready to be processed in object store */

final case class NotifySdesFileReadyRequest(
  informationType: String,
  file: NotifySdesFile,
  audit: NotifySdesAudit
)

final case class NotifySdesFile(
  recipientOrSender: Option[String],
  name: String,
  location: Option[String],
  checksum: NotifySdesFileReadyChecksum,
  size: Int,
  properties: Option[List[SdesProxyProperty]]
)

object NotifySdesFile:
  given Writes[NotifySdesFile] = Json.writes[NotifySdesFile]

object NotifySdesFileReadyRequest:
  given Writes[NotifySdesFileReadyRequest] = Json.writes[NotifySdesFileReadyRequest]

final case class NotifySdesFileReadyChecksum(
  algorithm: SdesChecksumAlgorithm,
  value: String
)

object NotifySdesFileReadyChecksum:
  given Writes[NotifySdesFileReadyChecksum] = Json.writes[NotifySdesFileReadyChecksum]

enum SdesChecksumAlgorithm:

  case md5
  case SHA512
  case SHA256

object SdesChecksumAlgorithm:
  given Format[SdesChecksumAlgorithm] = JsonFormatsFactory.makeEnumFormat[SdesChecksumAlgorithm]

final case class SdesProxyProperty(
  name: String,
  value: String
)

object SdesProxyProperty:
  given Writes[SdesProxyProperty] = Json.writes[SdesProxyProperty]

final case class NotifySdesAudit(
  correlationId: String
)

object NotifySdesAudit:
  given Writes[NotifySdesAudit] = Json.writes[NotifySdesAudit]
