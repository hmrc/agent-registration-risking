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
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationForRiskingStatus
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.risking.PersonReference
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationrisking.util.BooleanExtensions.convertBooleanToStringRepresentation
import uk.gov.hmrc.agentregistrationrisking.util.MinervaDateFormats.convertToMinervaDateString

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
      amlExpiryDate.map(convertToMinervaDateString).getOrElse(""),
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
    case IndividualDateOfBirth.Provided(dob) => convertToMinervaDateString(dob)
    case IndividualDateOfBirth.FromCitizensDetails(dob) => convertToMinervaDateString(dob)
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

  def fromApplicationForRisking(applicationForRisking: ApplicationForRisking): RiskingFileDataRecord = this.apply(
    recordType = RecordType.Entity,
    resubmission = isResubmission(applicationForRisking.status),
    applicationReference = Some(applicationForRisking.applicationReference),
    applicantName = Some(applicationForRisking.applicantName),
    applicantPhone = applicationForRisking.applicantPhone,
    applicantEmail = applicationForRisking.applicantEmail,
    entityType = Some(applicationForRisking.entityType),
    entityIdentifier = Some(applicationForRisking.entityIdentifier),
    crn = applicationForRisking.crn,
    vrns = applicationForRisking.vrns,
    payeRefs = applicationForRisking.payeRefs,
    amlSupervisoryBody = Some(applicationForRisking.amlSupervisoryBody),
    amlRegNumber = Some(applicationForRisking.amlRegNumber),
    amlExpiryDate = applicationForRisking.amlExpiryDate,
    amlEvidence = applicationForRisking.amlEvidence,
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

  def fromIndividualForRisking(individualForRisking: IndividualForRisking): RiskingFileDataRecord = this.apply(
    recordType = RecordType.Individual,
    resubmission = isResubmission(individualForRisking.status),
    applicationReference = None,
    applicantName = None,
    applicantPhone = None,
    applicantEmail = None,
    entityType = None,
    entityIdentifier = None,
    crn = None,
    vrns = individualForRisking.vrns,
    payeRefs = individualForRisking.payeRefs,
    amlSupervisoryBody = None,
    amlRegNumber = None,
    amlExpiryDate = None,
    amlEvidence = None,
    personReference = Some(individualForRisking.personReference),
    individualCompaniesHouseName = individualForRisking.companiesHouseName,
    individualCompaniesHouseDateOfBirth = individualForRisking.companiesHouseDateOfBirth,
    individualProvidedName = Some(individualForRisking.providedName),
    individualProvidedDateOfBirth = Some(individualForRisking.providedDateOfBirth),
    individualNino = individualForRisking.nino,
    individualSaUtr = individualForRisking.saUtr,
    individualPhoneNumber = Some(individualForRisking.phoneNumber),
    individualEmail = Some(individualForRisking.email),
    providedByApplicant = Some(individualForRisking.providedByApplicant),
    passedIV = Some(individualForRisking.passedIV)
  )

  private def isResubmission(status: ApplicationForRiskingStatus): Boolean = status === ApplicationForRiskingStatus.ReadyForResubmission

  implicit val format: OFormat[RiskingFileDataRecord] = Json.format[RiskingFileDataRecord]
