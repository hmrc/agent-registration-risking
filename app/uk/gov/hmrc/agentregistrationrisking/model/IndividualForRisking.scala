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

import org.apache.pekko.util.Helpers.Requiring
import play.api.libs.json.Json
import play.api.libs.json.OFormat
import play.api.libs.json.__
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistration.shared.individual.IndividualDateOfBirth
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.util.OptionalListExtensions.*

import java.time.LocalDate

final case class IndividualForRisking(
  personReference: PersonReference,
  status: ApplicationStatus = ApplicationStatus.ReadyForSubmission,
  vrns: String,
  payeRefs: String,
  companiesHouseName: Option[String],
  companiesHouseDateOfBirth: Option[LocalDate],
  providedName: IndividualName,
  providedDateOfBirth: IndividualDateOfBirth,
  nino: Option[IndividualNino],
  saUtr: Option[IndividualSaUtr],
  phoneNumber: TelephoneNumber,
  email: EmailAddress,
  providedByApplicant: Boolean,
  passedIV: Boolean,
  failures: Option[List[Failure]] = None
) {}

extension (individuals: List[IndividualProvidedDetails])
  def toIndividualsForRisking: List[IndividualForRisking] = individuals.map(IndividualForRisking.fromIndividualProvidedDetails)

object IndividualForRisking {

  implicit val format: OFormat[IndividualForRisking] = Json.format[IndividualForRisking]

  def fromIndividualProvidedDetails(individual: IndividualProvidedDetails): IndividualForRisking = {
    apply(
      personReference = PersonReference(individual._id.value),
      vrns = transformToCommaSeparatedString(individual.vrns.map(_.map(_.value))),
      payeRefs = transformToCommaSeparatedString(individual.payeRefs.map(_.map(_.value))),
      companiesHouseName = None,
      companiesHouseDateOfBirth = None,
      providedName = individual.individualName,
      providedDateOfBirth = individual.getDateOfBirth,
      nino = individual.individualNino,
      saUtr = individual.individualSaUtr,
      phoneNumber = individual.getTelephoneNumber,
      email = individual.getEmailAddress.emailAddress,
      providedByApplicant = true,
      passedIV = true
    )
  }

}
