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
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement.Agreed
import uk.gov.hmrc.agentregistration.shared.UserRole.Partner
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentDetails
import uk.gov.hmrc.agentregistration.shared.amls.AmlsEvidence
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsLlp
import uk.gov.hmrc.agentregistration.shared.businessdetails.CompanyProfile
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantEmailAddress
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLessOfficers
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase
import uk.gov.hmrc.agentregistration.shared.upload.UploadId
import uk.gov.hmrc.objectstore.client.Path.File

import java.time.Instant
import java.time.LocalDate

//TODO - replace with share TdAgentApplicationLlp
trait TdAgentApplication { dependencies: TdBase =>

  private val createdAt: Instant = dependencies.nowAsInstant

  // TODO - create TdAgentApplicationLlp after risking state
  val llpApplication: AgentApplicationLlp = AgentApplicationLlp(
    _id = agentApplicationId,
    internalUserId = internalUserId,
    applicantCredentials = credentials,
    linkId = linkId,
    groupId = groupId,
    createdAt = createdAt,
    submittedAt = Some(createdAt),
    applicationState = ApplicationState.SentForRisking,
    userRole = Some(Partner),
    businessDetails = Some(BusinessDetailsLlp(
      companyProfile = CompanyProfile(
        companyNumber = crn,
        companyName = "Test LLP",
        dateOfIncorporation = Some(LocalDate.now()),
        unsanitisedCHROAddress = None
      ),
      saUtr = dependencies.saUtr,
      safeId = SafeId("AARN1234567")
    )),
    applicantContactDetails = Some(ApplicantContactDetails(
      applicantName = applicantName,
      telephoneNumber = Some(telephoneNumber),
      applicantEmailAddress = Some(ApplicantEmailAddress(applicantEmailAddress, isVerified = true))
    )),
    amlsDetails = Some(AmlsDetails(
      supervisoryBody = amlsCode,
      amlsRegistrationNumber = Some(amlsRegistrationNumber),
      amlsExpiryDate = Some(LocalDate.parse(dateString)),
      amlsEvidence = Some(AmlsEvidence(
        UploadId("evidence-reference-123"),
        "certificate.pdf",
        File("/test.txt")
      ))
    )),
    agentDetails = Some(AgentDetails(
      businessName = agentBusinessName,
      telephoneNumber = Some(agentTelephoneNumber),
      agentEmailAddress = Some(agentVerifiedEmailAddress),
      agentCorrespondenceAddress = None
    )),
    refusalToDealWithCheckResult = None,
    companyStatusCheckResult = None,
    hmrcStandardForAgentsAgreed = Agreed,
    numberOfIndividuals = Some(FiveOrLessOfficers(
      numberOfCompaniesHouseOfficers = 2,
      isCompaniesHouseOfficersListCorrect = true
    )),
    hasOtherRelevantIndividuals = Some(true),
    vrns = Some(List(vrn, vrn)),
    payeRefs = Some(List(payeRef, payeRef))
  )

}
