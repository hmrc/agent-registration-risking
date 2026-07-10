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
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.individual.IndividualDateOfBirth
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.amls.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistration.shared.amls.AmlsSupervisoryBodyCode
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.AmlsEvidenceData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicantContactDetailsData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicationData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.IndividualData
import uk.gov.hmrc.agentregistration.shared.util.OptionalListExtensions.transformToCommaSeparatedString
import uk.gov.hmrc.agentregistrationrisking.util.BooleanExtensions.convertBooleanToStringRepresentation
import uk.gov.hmrc.agentregistrationrisking.util.MinervaDateFormats.asMinervaDate
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.model.amls.AmlsEvidenceUrl

import java.time.LocalDate

final case class RiskingFileDataRecord(
  recordType: RecordType,
  resubmission: Boolean,
  applicationReference: Option[ApplicationReference],
  applicantName: Option[ApplicantName],
  applicantPhone: Option[TelephoneNumber],
  applicantEmail: Option[EmailAddress],
  entityType: Option[BusinessType],
  entityIdentifier: Option[Utr],
  crn: Option[Crn],
  vrns: String,
  payeRefs: String,
  amlSupervisoryBody: Option[AmlsSupervisoryBodyCode],
  amlRegNumber: Option[AmlsRegistrationNumber],
  amlExpiryDate: Option[LocalDate],
  amlEvidence: Option[AmlsEvidenceUrl],
  personReference: Option[PersonReference],
  individualCompaniesHouseName: Option[String],
  individualCompaniesHouseDateOfBirth: Option[LocalDate],
  individualProvidedName: Option[IndividualName],
  individualProvidedDateOfBirth: Option[IndividualDateOfBirth],
  individualNino: Option[IndividualNino],
  individualSaUtr: Option[IndividualSaUtr],
  individualPhoneNumber: Option[TelephoneNumber],
  individualEmail: Option[EmailAddress],
  providedByApplicant: Option[Boolean],
  passedIV: Option[Boolean]
) {

  def toPipeDelimitedString: String =
    val fields: Seq[String] = Seq(
      "01",
      recordType.toString,
      convertBooleanToStringRepresentation(resubmission),
      applicationReference.map(_.value).getOrElse(""),
      applicantName.map(_.value).getOrElse(""),
      applicantPhone.map(_.value).getOrElse(""),
      applicantEmail.map(_.value).getOrElse(""),
      entityType.map(_.toString).getOrElse(""),
      entityIdentifier.map(_.value).getOrElse(""),
      crn.map(_.value).getOrElse(""),
      vrns,
      payeRefs,
      amlSupervisoryBody.map(_.value).getOrElse(""),
      amlRegNumber.map(_.value).getOrElse(""),
      amlExpiryDate.map(asMinervaDate).getOrElse(""),
      amlEvidence.map(_.value).getOrElse(""),
      personReference.map(_.value).getOrElse(""),
      individualCompaniesHouseName.getOrElse(""),
      individualCompaniesHouseDateOfBirth.map(_.toString).getOrElse(""),
      individualProvidedName.map(_.value).getOrElse(""),
      getDobString,
      getNinoString,
      getSaUtrString,
      individualPhoneNumber.map(_.value).getOrElse(""),
      individualEmail.map(_.value).getOrElse(""),
      providedByApplicant.map(convertBooleanToStringRepresentation).getOrElse(""),
      passedIV.map(convertBooleanToStringRepresentation).getOrElse("")
    )
    fields.mkString("|")

  private def getDobString: String = individualProvidedDateOfBirth.map {
    case IndividualDateOfBirth.Provided(dob) => asMinervaDate(dob)
    case IndividualDateOfBirth.ApplicantProvided(dob) => asMinervaDate(dob)
    case IndividualDateOfBirth.FromCitizensDetails(dob) => asMinervaDate(dob)
  }.getOrElse("")

  private def getNinoString: String = individualNino.map {
    case IndividualNino.Provided(dob) => dob.value
    case IndividualNino.FromAuth(dob) => dob.value
    case IndividualNino.NotProvided => ""
  }.getOrElse("")

  private def getSaUtrString: String = individualSaUtr.map {
    case IndividualSaUtr.Provided(dob) => dob.value
    case IndividualSaUtr.FromAuth(dob) => dob.value
    case IndividualSaUtr.FromCitizenDetails(dob) => dob.value
    case IndividualSaUtr.NotProvided => ""
  }.getOrElse("")

}

object RiskingFileDataRecord:

  def fromApplicationForRisking(
    applicationForRisking: ApplicationForRisking,
    amlsEvidenceBaseUrl: String
  ): RiskingFileDataRecord =
    val applicationData: ApplicationData = applicationForRisking.applicationData
    val contactDetails: ApplicantContactDetailsData = applicationData.applicantContactDetails
    RiskingFileDataRecord(
      recordType = RecordType.Entity,
      resubmission = applicationForRisking.isResubmission,
      applicationReference = Some(applicationData.applicationReference),
      applicantName = Some(contactDetails.applicantName),
      applicantPhone = Some(contactDetails.telephoneNumber),
      applicantEmail = Some(contactDetails.applicantEmailAddress),
      entityType = Some(applicationData.businessType),
      entityIdentifier = Some(applicationData.utr),
      crn = applicationData.crn,
      vrns = applicationData.vrns.map(_.value).mkString(","),
      payeRefs = applicationData.payeRefs.map(_.value).mkString(","),
      amlSupervisoryBody = Some(applicationData.amlsDetails.supervisoryBody),
      amlRegNumber = Some(applicationData.amlsDetails.amlsRegistrationNumber),
      amlExpiryDate = None, // we don't capture the AML expiry date in the application
      amlEvidence = applicationData.amlsDetails.amlsEvidence.map(_.makeAmlsEvidenceUrl(amlsEvidenceBaseUrl)),
      personReference = None,
      individualCompaniesHouseName = None,
      individualCompaniesHouseDateOfBirth = None,
      individualProvidedName = None,
      individualProvidedDateOfBirth = None,
      individualNino = None,
      individualSaUtr = None,
      individualPhoneNumber = None,
      individualEmail = None,
      providedByApplicant = None,
      passedIV = None
    )

  def fromIndividualForRisking(individualForRisking: IndividualForRisking): RiskingFileDataRecord =
    val individualData: IndividualData = individualForRisking.individualData
    this.apply(
      recordType = RecordType.Individual,
      resubmission = individualForRisking.isResubmission,
      applicationReference = None,
      applicantName = None,
      applicantPhone = None,
      applicantEmail = None,
      entityType = None,
      entityIdentifier = None,
      crn = None,
      vrns = individualData.vrns.map(_.value).mkString(","),
      payeRefs = individualData.payeRefs.map(_.value).mkString(","),
      amlSupervisoryBody = None,
      amlRegNumber = None,
      amlExpiryDate = None,
      amlEvidence = None,
      personReference = Some(individualData.personReference),
      individualCompaniesHouseName = individualData.companiesHouseName,
      individualCompaniesHouseDateOfBirth = individualData.companiesHouseDateOfBirth,
      individualProvidedName = Some(individualData.individualName),
      individualProvidedDateOfBirth = Some(individualData.individualDateOfBirth),
      individualNino = Some(individualData.individualNino),
      individualSaUtr = Some(individualData.individualSaUtr),
      individualPhoneNumber = Some(individualData.telephoneNumber),
      individualEmail = Some(individualData.emailAddress),
      providedByApplicant = Some(individualData.providedByApplicant),
      passedIV = Some(individualData.passedIv)
    )

  private def getMaybeCrn(agentApplication: AgentApplication): Option[Crn] =
    agentApplication match
      case a: AgentApplicationLimitedCompany => Some(a.getBusinessDetails.companyProfile.companyNumber)
      case a: AgentApplicationLimitedPartnership => Some(a.getBusinessDetails.companyProfile.companyNumber)
      case a: AgentApplicationLlp => Some(a.getBusinessDetails.companyProfile.companyNumber)
      case a: AgentApplicationScottishLimitedPartnership => Some(a.getBusinessDetails.companyProfile.companyNumber)
      case _ => None

  extension (amlsEvidence: AmlsEvidenceData)
    def makeAmlsEvidenceUrl(baseUrl: String): AmlsEvidenceUrl =
      val baseUrlNormalised: String =
        if baseUrl.endsWith("/")
        then baseUrl
        else s"$baseUrl/"
      AmlsEvidenceUrl(s"$baseUrlNormalised${amlsEvidence.fileUploadReference.value}")
