/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationrisking.testsupport.testdata

import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.amls.AmlsEvidence
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsLlp
import uk.gov.hmrc.agentregistration.shared.businessdetails.CompanyProfile
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantEmailAddress
import uk.gov.hmrc.agentregistration.shared.risking.RiskingProgressForApplicant
import uk.gov.hmrc.agentregistration.shared.risking.IndividualRiskingResponse
import uk.gov.hmrc.agentregistration.shared.risking.RiskingStatus
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase
import uk.gov.hmrc.agentregistration.shared.upload.UploadId
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRiskingId

import java.time.LocalDate

trait TdApplicationForRisking { dependencies: TdBase =>

  val llpApplicationForRisking: ApplicationForRisking = ApplicationForRisking(
    _id = ApplicationForRiskingId(randomId()),
    agentApplication = AgentApplicationLlp(
      _id = dependencies.agentApplicationId,
      applicationReference = ApplicationReference("ABC123456"),
      internalUserId = dependencies.internalUserId,
      applicantCredentials = dependencies.credentials,
      linkId = dependencies.linkId,
      groupId = dependencies.groupId,
      createdAt = dependencies.nowAsInstant,
      submittedAt = Some(dependencies.nowAsInstant),
      applicationState = ApplicationState.SentForRisking,
      userRole = Some(UserRole.Partner),
      businessDetails = Some(BusinessDetailsLlp(
        companyProfile = CompanyProfile(
          companyNumber = dependencies.crn,
          companyName = "Test Company Name",
          dateOfIncorporation = Some(LocalDate.parse(dependencies.dateString)),
          unsanitisedCHROAddress = None
        ),
        saUtr = dependencies.saUtr,
        safeId = SafeId("X0_SAFE_ID_0X")
      )),
      applicantContactDetails = Some(ApplicantContactDetails(
        applicantName = dependencies.applicantName,
        telephoneNumber = Some(dependencies.telephoneNumber),
        applicantEmailAddress = Some(ApplicantEmailAddress(dependencies.applicantEmailAddress, isVerified = true))
      )),
      amlsDetails = Some(AmlsDetails(
        supervisoryBody = dependencies.amlsCode,
        amlsRegistrationNumber = Some(dependencies.amlsRegistrationNumber),
        amlsEvidence = Some(AmlsEvidence(
          UploadId("evidence-reference-123"),
          "certificate.pdf",
          uk.gov.hmrc.objectstore.client.Path.File("test.txt")
        ))
      )),
      agentDetails = Some(dependencies.completeAgentDetails),
      refusalToDealWithCheckResult = Some(CheckResult.Pass),
      companyStatusCheckResult = Some(CheckResult.Pass),
      hmrcStandardForAgentsAgreed = StateOfAgreement.Agreed,
      numberOfIndividuals = None,
      hasOtherRelevantIndividuals = Some(false),
      vrns = Some(List(dependencies.vrn, dependencies.vrn)),
      payeRefs = Some(List(dependencies.payeRef, dependencies.payeRef))
    ),
    createdAt = dependencies.nowAsInstant,
    lastUpdatedAt = dependencies.nowAsInstant,
    riskingFileName = None,
    failures = None,
    isSubscribed = false,
    isEmailSent = false
  )

  def applicationRiskingResponseReadyForSubmission(
    applicationReference: ApplicationReference,
    personReference: PersonReference
  ) = RiskingProgressForApplicant(
    applicationReference = applicationReference,
    status = RiskingStatus.ReadyForSubmission,
    isSubscribed = false,
    individuals = List(IndividualRiskingResponse(
      personReference = personReference,
      providedName = dependencies.individualName,
      failures = None
    )),
    failures = None
  )

}
