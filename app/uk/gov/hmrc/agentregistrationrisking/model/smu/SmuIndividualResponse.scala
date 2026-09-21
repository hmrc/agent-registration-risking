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
import uk.gov.hmrc.agentregistration.shared.amls.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistration.shared.amls.AmlsSupervisoryBodyCode
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.individual.*
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicationData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.IndividualData
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking

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
    individual: IndividualForRisking,
    application: ApplicationForRisking
  ): SmuIndividualResponse = SmuIndividualResponse(
    IndividualForSmuViewer.make(individual),
    EntityForSmuViewer.make(application)
  )

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

    private[SmuIndividualResponse] def make(individual: IndividualForRisking): IndividualForSmuViewer =
      val individualData: IndividualData = individual.individualData
      IndividualForSmuViewer(
        personReference = individualData.personReference,
        resubmission = individual.isResubmission,
        passedIdentityVerification = individualData.passedIv,
        detailsProvidedByApplicant = individualData.providedByApplicant,
        individualName = individualData.individualName,
        individualDateOfBirth =
          individualData.individualDateOfBirth match
            case IndividualDateOfBirth.Provided(date) => date
            case IndividualDateOfBirth.FromCitizensDetails(date) => date
            case IndividualDateOfBirth.ApplicantProvided(date) => date
        ,
        individualNino =
          individualData.individualNino match
            case IndividualNino.Provided(nino) => Some(nino)
            case IndividualNino.FromAuth(nino) => Some(nino)
            case IndividualNino.NotProvided => None
        ,
        individualSaUtr =
          individualData.individualSaUtr match
            case IndividualSaUtr.Provided(saUtr) => Some(saUtr)
            case IndividualSaUtr.FromAuth(saUtr) => Some(saUtr)
            case IndividualSaUtr.FromCitizenDetails(saUtr) => Some(saUtr)
            case IndividualSaUtr.NotProvided => None
        ,
        payeRefs = individualData.payeRefs,
        vrns = individualData.vrns,
        telephoneNumber = individualData.telephoneNumber,
        emailAddress = individualData.emailAddress
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

    private[SmuIndividualResponse] def make(application: ApplicationForRisking): EntityForSmuViewer =
      val applicationData: ApplicationData = application.applicationData
      EntityForSmuViewer(
        applicationReference = applicationData.applicationReference,
        resubmission = application.isResubmission,
        applicantName = applicationData.applicantContactDetails.applicantName,
        businessType = applicationData.businessType,
        utr = applicationData.utr,
        payeRefs = applicationData.payeRefs,
        vrns = applicationData.vrns,
        crn = applicationData.crn,
        amlsSupervisoryBody = applicationData.amlsDetails.supervisoryBody,
        amlsRegNumber = applicationData.amlsDetails.amlsRegistrationNumber,
        amlsExpiryDate = None,
        amlsEvidenceReferenceId = applicationData.amlsDetails.amlsEvidence.map(_.fileUploadReference.value),
        applicantPhone = applicationData.applicantContactDetails.telephoneNumber,
        applicantEmail = applicationData.applicantContactDetails.applicantEmailAddress
      )
