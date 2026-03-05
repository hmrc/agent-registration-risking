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
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistration.shared.individual.IndividualDateOfBirth
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.util.OptionalListExtensions.*
import uk.gov.hmrc.agentregistrationrisking.util.BooleanExtensions.*
import uk.gov.hmrc.agentregistrationrisking.util.MinervaDateFormats.*
import java.time.format.DateTimeFormatter.ofPattern

import java.time.LocalDate

final case class IndividualForRisking(
  personReference: PersonReference,
  status: ApplicationForRiskingStatus,
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
  failures: Option[List[Failure]]
)

extension (individuals: List[IndividualProvidedDetails])
  def toIndividualsForRisking: List[IndividualForRisking] = individuals.map(IndividualForRisking.fromIndividualProvidedDetails)

object IndividualForRisking:

  implicit val format: OFormat[IndividualForRisking] = Json.format[IndividualForRisking]
  def fromIndividualProvidedDetails(individual: IndividualProvidedDetails): IndividualForRisking = apply(
    personReference = PersonReference(individual._id.value),
    status = ApplicationForRiskingStatus.ReadyForSubmission,
    vrns = transformToCommaSeparatedString(individual.vrns.map(_.map(_.value))),
    payeRefs = transformToCommaSeparatedString(individual.payeRefs.map(_.map(_.value))),
    companiesHouseName = None, // We don't currently store the name retrieved from companies house
    companiesHouseDateOfBirth = None, // As above
    providedName = individual.individualName,
    providedDateOfBirth = individual.getDateOfBirth,
    nino = individual.individualNino,
    saUtr = individual.individualSaUtr,
    phoneNumber = individual.getTelephoneNumber,
    email = individual.getEmailAddress.emailAddress,
    providedByApplicant = true, // Not currently possible for anyone other than the applicant to provide details
    passedIV = true, // We don't currently log whether the applicant passed IV or not, this will come later
    failures = None
  )
