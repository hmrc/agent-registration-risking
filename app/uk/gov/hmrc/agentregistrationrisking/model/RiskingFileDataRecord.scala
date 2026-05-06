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
import uk.gov.hmrc.agentregistration.shared.amls.AmlsEvidence
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.individual.IndividualDateOfBirth
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.util.OptionalListExtensions.transformToCommaSeparatedString
import uk.gov.hmrc.agentregistrationrisking.util.BooleanExtensions.convertBooleanToStringRepresentation
import uk.gov.hmrc.agentregistrationrisking.util.MinervaDateFormats.asMinervaDate

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
  amlSupervisoryBody: Option[AmlsCode],
  amlRegNumber: Option[AmlsRegistrationNumber],
  amlExpiryDate: Option[LocalDate],
  amlEvidence: Option[AmlsEvidence],
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
    val fields = Seq(
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
      amlEvidence.map(_.uploadId.value).getOrElse(""),
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

  def fromApplicationForRisking(applicationForRisking: ApplicationForRisking): RiskingFileDataRecord =
    val app = applicationForRisking.agentApplication
    val contactDetails = app.getApplicantContactDetails
    this.apply(
      recordType = RecordType.Entity,
      resubmission = applicationForRisking.failures.isDefined,
      applicationReference = Some(app.applicationReference),
      applicantName = Some(contactDetails.applicantName),
      applicantPhone = contactDetails.telephoneNumber,
      applicantEmail = contactDetails.applicantEmailAddress.map(_.emailAddress),
      entityType = Some(app.businessType),
      entityIdentifier = Some(app.getUtr),
      crn = getMaybeCrn(app),
      vrns = transformToCommaSeparatedString(app.vrns.map(_.map(_.value))),
      payeRefs = transformToCommaSeparatedString(app.payeRefs.map(_.map(_.value))),
      amlSupervisoryBody = Some(app.getAmlsDetails.supervisoryBody),
      amlRegNumber = Some(app.getAmlsDetails.getRegistrationNumber),
      amlExpiryDate = None, // we don't capture the AML expiry date in the application
      amlEvidence = app.getAmlsDetails.amlsEvidence,
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
    val details = individualForRisking.individualProvidedDetails
    this.apply(
      recordType = RecordType.Individual,
      resubmission = individualForRisking.failures.isDefined,
      applicationReference = None,
      applicantName = None,
      applicantPhone = None,
      applicantEmail = None,
      entityType = None,
      entityIdentifier = None,
      crn = None,
      vrns = transformToCommaSeparatedString(details.vrns.map(_.map(_.value))),
      payeRefs = transformToCommaSeparatedString(details.payeRefs.map(_.map(_.value))),
      amlSupervisoryBody = None,
      amlRegNumber = None,
      amlExpiryDate = None,
      amlEvidence = None,
      personReference = Some(details.personReference),
      individualCompaniesHouseName = individualForRisking.companiesHouseName,
      individualCompaniesHouseDateOfBirth = individualForRisking.companiesHouseDateOfBirth,
      individualProvidedName = Some(details.individualName),
      individualProvidedDateOfBirth = details.individualDateOfBirth,
      individualNino = details.individualNino,
      individualSaUtr = details.individualSaUtr,
      individualPhoneNumber = details.telephoneNumber,
      individualEmail = details.emailAddress.map(_.emailAddress),
      providedByApplicant = Some(individualForRisking.providedByApplicant),
      passedIV = details.passedIv
    )

  private def getMaybeCrn(agentApplication: AgentApplication): Option[Crn] =
    agentApplication match
      case a: AgentApplicationLimitedCompany => Some(a.getBusinessDetails.companyProfile.companyNumber)
      case a: AgentApplicationLimitedPartnership => Some(a.getBusinessDetails.companyProfile.companyNumber)
      case a: AgentApplicationLlp => Some(a.getBusinessDetails.companyProfile.companyNumber)
      case a: AgentApplicationScottishLimitedPartnership => Some(a.getBusinessDetails.companyProfile.companyNumber)
      case _ => None

  implicit val format: OFormat[RiskingFileDataRecord] = Json.format[RiskingFileDataRecord]
