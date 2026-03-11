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
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentBusinessName
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentCorrespondenceAddress
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentDetails
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentEmailAddress
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentTelephoneNumber
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentVerifiedEmailAddress
import uk.gov.hmrc.agentregistration.shared.amls.AmlsEvidence
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsLlp
import uk.gov.hmrc.agentregistration.shared.businessdetails.CompanyProfile
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantEmailAddress
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLess
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLessOfficers
import uk.gov.hmrc.agentregistration.shared.upload.UploadId
import uk.gov.hmrc.objectstore.client.Path.File

import java.time.Instant
import java.time.LocalDate

trait TdAgentApplication { dependencies: TdBase =>

  private val createdAt: Instant = dependencies.instant

  val llpApplication: AgentApplicationLlp = AgentApplicationLlp(
    _id = agentApplicationId,
    internalUserId = internalUserId,
    linkId = linkId,
    groupId = groupId,
    createdAt = createdAt,
    submittedAt = Some(createdAt),
    applicationState = ApplicationState.SentForRisking,
    userRole = Some(Partner),
    businessDetails = Some(BusinessDetailsLlp(
      companyProfile = CompanyProfile(
        companyNumber = Crn(crn),
        companyName = "Test LLP",
        dateOfIncorporation = Some(LocalDate.now()),
        unsanitisedCHROAddress = None
      ),
      saUtr = SaUtr(dependencies.utr.value),
      safeId = SafeId("AARN1234567")
    )),
    applicantContactDetails = Some(ApplicantContactDetails(
      applicantName = ApplicantName(applicantName),
      telephoneNumber = Some(TelephoneNumber(telephoneNumber)),
      applicantEmailAddress = Some(ApplicantEmailAddress(EmailAddress(email), isVerified = true))
    )),
    amlsDetails = Some(AmlsDetails(
      supervisoryBody = AmlsCode(amlsCode),
      amlsRegistrationNumber = Some(AmlsRegistrationNumber(amlsRegistrationNumber)),
      amlsExpiryDate = Some(LocalDate.parse(dateString)),
      amlsEvidence = Some(AmlsEvidence(
        UploadId("evidence-reference-123"),
        "certificate.pdf",
        File("/test.txt")
      ))
    )),
    agentDetails = Some(AgentDetails(
      businessName = AgentBusinessName(agentBusinessName, None),
      telephoneNumber = Some(AgentTelephoneNumber(telephoneNumber, None)),
      agentEmailAddress = Some(AgentVerifiedEmailAddress(AgentEmailAddress(email, None), isVerified = true)),
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
    vrns = Some(List(Vrn(vrn), Vrn(vrn))),
    payeRefs = Some(List(PayeRef(payeRef), PayeRef(payeRef)))
  )

}
