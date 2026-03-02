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
import uk.gov.hmrc.agentregistration.shared.amls.AmlsEvidence
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedCompany
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.AmlsCode
import uk.gov.hmrc.agentregistration.shared.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.Crn
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.risking.SubmitForRiskingRequest
import uk.gov.hmrc.agentregistration.shared.util.OptionalListExtensions.transformToCommaSeparatedString

import java.time.Instant
import java.time.LocalDate

final case class ApplicationForRisking(
  applicationReference: ApplicationReference,
  status: ApplicationForRiskingStatus,
  createdAt: Instant,
  uploadedAt: Option[Instant],
  fileName: Option[String],
  applicantName: ApplicantName,
  applicantPhone: Option[TelephoneNumber],
  applicantEmail: Option[EmailAddress],
  entityType: BusinessType,
  entityIdentifier: Utr,
  crn: Option[Crn],
  vrns: String,
  payeRefs: String,
  amlSupervisoryBody: AmlsCode,
  amlRegNumber: AmlsRegistrationNumber,
  amlExpiryDate: Option[LocalDate],
  amlEvidence: Option[AmlsEvidence],
  individuals: List[IndividualForRisking],
  failures: Option[List[Failure]]
) {}

extension (submitForRiskingRequest: SubmitForRiskingRequest)

  def toApplicationForRisking: ApplicationForRisking =
    val application = submitForRiskingRequest.agentApplication
    ApplicationForRisking(
      applicationReference = ApplicationReference(application.agentApplicationId.value),
      status = ApplicationForRiskingStatus.ReadyForSubmission,
      createdAt = Instant.now(),
      uploadedAt = None,
      fileName = None,
      applicantName = application.getApplicantContactDetails.applicantName,
      applicantPhone = application.getApplicantContactDetails.telephoneNumber,
      applicantEmail = application.getApplicantContactDetails.applicantEmailAddress.map(_.emailAddress),
      entityType = application.businessType,
      entityIdentifier = application.getUtr,
      crn = getMaybeCrn(application),
      vrns = transformToCommaSeparatedString(application.vrns.map(_.map(_.value))),
      payeRefs = transformToCommaSeparatedString(application.payeRefs.map(_.map(_.value))),
      amlSupervisoryBody = application.getAmlsDetails.supervisoryBody,
      amlRegNumber = application.getAmlsDetails.getRegistrationNumber,
      amlExpiryDate = application.getAmlsDetails.amlsExpiryDate,
      amlEvidence = application.getAmlsDetails.amlsEvidence,
      individuals = submitForRiskingRequest.individuals.toIndividualsForRisking,
      failures = None
    )

  private def getMaybeCrn(agentApplication: AgentApplication): Option[Crn] =
    agentApplication match {
      case a: AgentApplicationLimitedCompany => Some(a.getBusinessDetails.companyProfile.companyNumber)
      case a: AgentApplicationLimitedPartnership => Some(a.getBusinessDetails.companyProfile.companyNumber)
      case a: AgentApplicationLlp => Some(a.getBusinessDetails.companyProfile.companyNumber)
      case a: AgentApplicationScottishLimitedPartnership => Some(a.getBusinessDetails.companyProfile.companyNumber)
      case _ => None
    }

object ApplicationForRisking:
  implicit val format: OFormat[ApplicationForRisking] = Json.format[ApplicationForRisking]
