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

import play.api.libs.json.{Json, OFormat}

case class Individual(
  personReference: String,
  status: ApplicationStatus = ApplicationStatus.ReadyForSubmission,
  vrns: Option[List[String]],
  payeRefs: Option[List[String]],
  companiesHouseName: Option[String],
  companiesHouseDateOfBirth: Option[String],
  providedName: String,
  providedDateOfBirth: String,
  nino: Option[String],
  saUtr: Option[String],
  phoneNumber: String,
  email: String,
  providedByApplicant: Boolean,
  passedIV: Boolean,
  failures: Option[List[Failure]]
) {}

object Individual {
  implicit val format: OFormat[Individual] = Json.format[Individual]
}
