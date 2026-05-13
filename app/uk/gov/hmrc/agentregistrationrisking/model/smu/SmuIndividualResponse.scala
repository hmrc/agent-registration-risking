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

package uk.gov.hmrc.agentregistrationrisking.model.smu

import play.api.libs.json.Json
import play.api.libs.json.OFormat
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsIncorporated
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.individual.*
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicationData

import java.time.LocalDate

/** Represents an individual along with all relevant details and verification-related information required by the SMU (Secure Management Unit) to verify that
  * person.
  *
  * This case class aggregates individual provided details and agent application details.
  */
final case class SmuIndividualResponse(
  individual: SmuIndividualResponse.Individual,
  entity: SmuIndividualResponse.Entity
)

object SmuIndividualResponse:

  given format: OFormat[SmuIndividualResponse] = Json.format[SmuIndividualResponse]

  def make(
    ipd: IndividualProvidedDetails,
    aa: ApplicationData
  ) = SmuIndividualResponse(Individual.make(ipd), Entity.make(aa))

  final case class Individual(
    personReference: PersonReference,
    resubmission: Boolean,
    passedIdentityVerification: Boolean,
    detailsProvidedByApplicant: Boolean,
    individualName: IndividualName,
    individualDateOfBirth: LocalDate,
    individualNino: Option[Nino],
    individualSaUtr: Option[SaUtr],
    payeRefs: Option[List[PayeRef]],
    vrns: Option[List[Vrn]],
    telephoneNumber: TelephoneNumber,
    emailAddress: EmailAddress
  )

  object Individual:

    given format: OFormat[Individual] = Json.format[Individual]

    private[SmuIndividualResponse] def make(ipd: IndividualProvidedDetails): Individual = Individual(
      personReference = ipd.personReference,
      // TODO update this once we have implemented resubmission flags
      resubmission = false,
      passedIdentityVerification = ipd.passedIv.contains(true),
      detailsProvidedByApplicant = ipd.providedByApplicant.contains(true),
      individualName = ipd.individualName,
      individualDateOfBirth =
        ipd.getDateOfBirth match
          case IndividualDateOfBirth.Provided(date) => date
          case IndividualDateOfBirth.FromCitizensDetails(date) => date
          case IndividualDateOfBirth.ApplicantProvided(date) => date
      ,
      individualNino =
        ipd.individualNino match
          case Some(IndividualNino.Provided(nino)) => Some(nino)
          case Some(IndividualNino.FromAuth(nino)) => Some(nino)
          case _ => None
      ,
      individualSaUtr =
        ipd.individualSaUtr match {
          case Some(IndividualSaUtr.Provided(saUtr)) => Some(saUtr)
          case Some(IndividualSaUtr.FromAuth(saUtr)) => Some(saUtr)
          case Some(IndividualSaUtr.FromCitizenDetails(saUtr)) => Some(saUtr)
          case _ => None
        },
      payeRefs = ipd.payeRefs,
      vrns = ipd.vrns,
      telephoneNumber = ipd.getTelephoneNumber,
      emailAddress = ipd.getEmailAddress.emailAddress
    )

  final case class Entity(
    applicationReference: ApplicationReference,
    resubmission: Boolean,
    applicantName: ApplicantName,
    businessType: BusinessType,
    utr: Utr,
    payeRefs: List[PayeRef],
    vrns: List[Vrn],
    crn: Option[Crn],
    amlsSupervisoryBody: AmlsCode,
    amlsRegNumber: AmlsRegistrationNumber,
    amlsExpiryDate: Option[LocalDate],
    amlsEvidenceReferenceId: Option[String],
    applicantPhone: TelephoneNumber,
    applicantEmail: EmailAddress
  )

  object Entity:

    given format: OFormat[Entity] = Json.format[Entity]

    private[SmuIndividualResponse] def make(
      aa: ApplicationData
    ): Entity = Entity(
      applicationReference = aa.applicationReference,
      // TODO update this once we have implemented resubmission flags
      resubmission = false,
      applicantName = aa.applicantContactDetails.applicantName,
      businessType = aa.businessType,
      utr = aa.utr,
      payeRefs = aa.payeRefs,
      vrns = aa.vrns,
      crn = aa.crn,
      amlsSupervisoryBody = aa.amlsDetails.supervisoryBody,
      amlsRegNumber = aa.amlsDetails.amlsRegistrationNumber,
      amlsExpiryDate = None,
      amlsEvidenceReferenceId = aa.amlsDetails.amlsEvidence.map(_.fileUploadReference.value),
      applicantPhone = aa.applicantContactDetails.telephoneNumber,
      applicantEmail = aa.applicantContactDetails.applicantEmailAddress
    )
