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
import uk.gov.hmrc.agentregistration.shared.amls.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistration.shared.amls.AmlsSupervisoryBodyCode
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.individual.*
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicationData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.IndividualData

import java.time.LocalDate

/** Represents an individual along with all relevant details and verification-related information required by the SMU (Secure Management Unit) to verify that
  * person.
  *
  * This case class aggregates individual provided details and agent application details.
  */
final case class SmuIndividualResponse(
  individual: SmuIndividualResponse.IndividualForSmuViewer,
  entity: SmuIndividualResponse.EntityForSmuViewer
)

object SmuIndividualResponse:

  given format: OFormat[SmuIndividualResponse] = Json.format[SmuIndividualResponse]

  def make(
    ipd: IndividualData,
    aa: ApplicationData
  ) = SmuIndividualResponse(IndividualForSmuViewer.make(ipd), EntityForSmuViewer.make(aa))

  final case class IndividualForSmuViewer(
    personReference: PersonReference,
    resubmission: Boolean,
    passedIdentityVerification: Boolean,
    detailsProvidedByApplicant: Boolean,
    individualName: IndividualName,
    individualDateOfBirth: LocalDate,
    individualNino: Option[Nino],
    individualSaUtr: Option[SaUtr],
    payeRefs: List[PayeRef],
    vrns: List[Vrn],
    telephoneNumber: TelephoneNumber,
    emailAddress: EmailAddress
  )

  object IndividualForSmuViewer:

    given format: OFormat[IndividualForSmuViewer] = Json.format[IndividualForSmuViewer]

    private[SmuIndividualResponse] def make(individual: IndividualData): IndividualForSmuViewer = IndividualForSmuViewer(
      personReference = individual.personReference,
      // TODO update this once we have implemented resubmission flags
      resubmission = false,
      passedIdentityVerification = individual.passedIv,
      detailsProvidedByApplicant = individual.providedByApplicant,
      individualName = individual.individualName,
      individualDateOfBirth =
        individual.individualDateOfBirth match
          case IndividualDateOfBirth.Provided(date) => date
          case IndividualDateOfBirth.FromCitizensDetails(date) => date
          case IndividualDateOfBirth.ApplicantProvided(date) => date
      ,
      individualNino =
        individual.individualNino match
          case IndividualNino.Provided(nino) => Some(nino)
          case IndividualNino.FromAuth(nino) => Some(nino)
          case IndividualNino.NotProvided => None
      ,
      individualSaUtr =
        individual.individualSaUtr match
          case IndividualSaUtr.Provided(saUtr) => Some(saUtr)
          case IndividualSaUtr.FromAuth(saUtr) => Some(saUtr)
          case IndividualSaUtr.FromCitizenDetails(saUtr) => Some(saUtr)
          case IndividualSaUtr.NotProvided => None
      ,
      payeRefs = individual.payeRefs,
      vrns = individual.vrns,
      telephoneNumber = individual.telephoneNumber,
      emailAddress = individual.emailAddress
    )

  final case class EntityForSmuViewer(
    applicationReference: ApplicationReference,
    resubmission: Boolean,
    applicantName: ApplicantName,
    businessType: BusinessType,
    utr: Utr,
    payeRefs: List[PayeRef],
    vrns: List[Vrn],
    crn: Option[Crn],
    amlsSupervisoryBody: AmlsSupervisoryBodyCode,
    amlsRegNumber: AmlsRegistrationNumber,
    amlsExpiryDate: Option[LocalDate],
    amlsEvidenceReferenceId: Option[String],
    applicantPhone: TelephoneNumber,
    applicantEmail: EmailAddress
  )

  object EntityForSmuViewer:

    given format: OFormat[EntityForSmuViewer] = Json.format[EntityForSmuViewer]

    private[SmuIndividualResponse] def make(
      aa: ApplicationData
    ): EntityForSmuViewer = EntityForSmuViewer(
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
