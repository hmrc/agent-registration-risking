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

import play.api.libs.json.Json
import play.api.libs.json.OFormat

import java.time.Instant
import java.time.LocalDate

case class ApplicationForRisking(
  applicationReference: ApplicationReference,
  status: ApplicationStatus = ApplicationStatus.ReadyForSubmission,
  createdAt: Instant = Instant.now(),
  uploadedAt: Option[Instant],
  fileName: Option[String],
  applicantName: String,
  applicantPhone: Option[String],
  applicantEmail: Option[String],
  entityType: EntityType,
  entityIdentifier: String,
  crn: Option[String],
  vrns: Option[List[String]],
  payeRefs: Option[List[String]],
  amlSupervisoryBody: String,
  amlRegNumber: String,
  amlExpiryDate: Option[LocalDate],
  amlEvidence: Option[String],
  individuals: Option[List[Individual]],
  failures: Option[List[Failure]]
) {}

object ApplicationForRisking {
  implicit val format: OFormat[ApplicationForRisking] = Json.format[ApplicationForRisking]
}
