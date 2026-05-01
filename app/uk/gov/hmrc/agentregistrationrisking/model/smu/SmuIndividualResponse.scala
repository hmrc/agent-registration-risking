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
import uk.gov.hmrc.objectstore.client.PresignedDownloadUrl

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
    aa: AgentApplication,
    amlsEvidencePresignedDownloadUrl: Option[PresignedDownloadUrl]
  ) = SmuIndividualResponse(Individual.make(ipd), Entity.make(aa, amlsEvidencePresignedDownloadUrl))

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
      passedIdentityVerification = ipd.getPassedIv,
      // TODO update this once we have details provided by applicant feature
      detailsProvidedByApplicant = false,
      individualName = ipd.individualName,
      individualDateOfBirth =
        ipd.getDateOfBirth match
          case IndividualDateOfBirth.Provided(date) => date
          case IndividualDateOfBirth.FromCitizensDetails(date) => date
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
    payeRefs: Option[List[PayeRef]],
    vrns: Option[List[Vrn]],
    crn: Option[Crn],
    amlsSupervisoryBody: AmlsCode,
    amlsRegNumber: AmlsRegistrationNumber,
    amlsExpiryDate: Option[LocalDate],
    amlsEvidencePresignedDownloadUrl: Option[String],
    applicantPhone: Option[TelephoneNumber],
    applicantEmail: Option[EmailAddress]
  )

  object Entity:

    given format: OFormat[Entity] = Json.format[Entity]

    private[SmuIndividualResponse] def make(
      aa: AgentApplication,
      amlsEvidencePresignedDownloadUrl: Option[PresignedDownloadUrl]
    ): Entity = Entity(
      applicationReference = aa.applicationReference,
      // TODO update this once we have implemented resubmission flags
      resubmission = false,
      applicantName = aa.getApplicantContactDetails.applicantName,
      businessType = aa.businessType,
      utr = aa.getUtr,
      payeRefs = aa.payeRefs,
      vrns = aa.vrns,
      crn =
        aa match
          case aa: IsIncorporated => Some(aa.getCrn)
          case _ => None
      ,
      amlsSupervisoryBody = aa.getAmlsDetails.supervisoryBody,
      amlsRegNumber = aa.getAmlsDetails.getRegistrationNumber,
      amlsExpiryDate = None,
      amlsEvidencePresignedDownloadUrl = amlsEvidencePresignedDownloadUrl.map(_.downloadUrl.toString),
      applicantPhone = aa.applicantContactDetails.map(_.getTelephoneNumber),
      applicantEmail = aa.applicantContactDetails.map(_.getVerifiedEmail)
    )
