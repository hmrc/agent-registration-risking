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
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.individual.*
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.objectstore.client.PresignedDownloadUrl

import java.time.LocalDate

/** Represents an individual along with all relevant details and verification-related information required by the SMU (Secure Management Unit) to verify that
  * person.
  *
  * This case class aggregates individual provided details and agent application details.
  */
final case class SmuIndividualResponse(
  entity: SmuIndividualResponse.Entity,
  individual: SmuIndividualResponse.Individual
)

object SmuIndividualResponse:

  given format: OFormat[SmuIndividualResponse] = Json.format[SmuIndividualResponse]

  def make(
    app: ApplicationForRisking,
    indi: IndividualForRisking,
    amlsEvidencePresignedDownloadUrl: PresignedDownloadUrl
  ) = SmuIndividualResponse(Entity.make(app, amlsEvidencePresignedDownloadUrl), Individual.make(indi))

  final case class Entity(
    applicationReference: ApplicationReference,
    resubmission: Boolean,
    applicantName: ApplicantName,
    businessType: BusinessType,
    utr: Utr,
    payeRefs: String,
    vrns: String,
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
      app: ApplicationForRisking,
      amlsEvidencePresignedDownloadUrl: PresignedDownloadUrl
    ): Entity = Entity(
      applicationReference = app.applicationReference,
      // TODO update this once we have implemented resubmission flags
      resubmission = false,
      applicantName = app.applicantName,
      businessType = app.entityType,
      utr = app.entityIdentifier,
      payeRefs = app.payeRefs,
      vrns = app.vrns,
      crn = app.crn,
      amlsSupervisoryBody = app.amlSupervisoryBody,
      amlsRegNumber = app.amlRegNumber,
      amlsExpiryDate = app.amlExpiryDate,
      amlsEvidencePresignedDownloadUrl = Some(amlsEvidencePresignedDownloadUrl.downloadUrl.toString),
      applicantPhone = app.applicantPhone,
      applicantEmail = app.applicantEmail
    )

  final case class Individual(
    personReference: PersonReference,
    resubmission: Boolean,
    passedIdentityVerification: Boolean,
    detailsProvidedByApplicant: Boolean,
    individualName: IndividualName,
    individualDateOfBirth: LocalDate,
    individualNino: Option[Nino],
    individualSaUtr: Option[SaUtr],
    payeRefs: String,
    vrns: String,
    telephoneNumber: TelephoneNumber,
    emailAddress: EmailAddress
  )

  object Individual:

    given format: OFormat[Individual] = Json.format[Individual]

    private[SmuIndividualResponse] def make(indi: IndividualForRisking): Individual = Individual(
      personReference = indi.personReference,
      // TODO update this once we have implemented resubmission flags
      resubmission = false,
      passedIdentityVerification = indi.passedIV,
      // TODO update this once we have details provided by applicant feature
      detailsProvidedByApplicant = false,
      individualName = indi.providedName,
      individualDateOfBirth =
        indi.providedDateOfBirth match
          case IndividualDateOfBirth.Provided(date) => date
          case IndividualDateOfBirth.FromCitizensDetails(date) => date
      ,
      individualNino =
        indi.nino match
          case Some(IndividualNino.Provided(nino)) => Some(nino)
          case Some(IndividualNino.FromAuth(nino)) => Some(nino)
          case _ => None
      ,
      individualSaUtr =
        indi.saUtr match {
          case Some(IndividualSaUtr.Provided(saUtr)) => Some(saUtr)
          case Some(IndividualSaUtr.FromAuth(saUtr)) => Some(saUtr)
          case Some(IndividualSaUtr.FromCitizenDetails(saUtr)) => Some(saUtr)
          case _ => None
        },
      payeRefs = indi.payeRefs,
      vrns = indi.vrns,
      telephoneNumber = indi.phoneNumber,
      emailAddress = indi.email
    )
