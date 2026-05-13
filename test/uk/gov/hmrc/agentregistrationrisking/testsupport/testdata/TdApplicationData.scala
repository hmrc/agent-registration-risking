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

package uk.gov.hmrc.agentregistrationrisking.testsupport.testdata

import uk.gov.hmrc.agentregistration.shared.AmlsCode
import uk.gov.hmrc.agentregistration.shared.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.CheckResult
import uk.gov.hmrc.agentregistration.shared.Crn
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.GroupId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.PayeRef
import uk.gov.hmrc.agentregistration.shared.SafeId
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistration.shared.Vrn
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentBusinessName
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentCorrespondenceAddress
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentEmailAddress
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentTelephoneNumber
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicationData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.AgentDetailsData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.AmlsDetailsData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.AmlsEvidenceData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicantContactDetailsData
import uk.gov.hmrc.agentregistration.shared.upload.FileUploadReference
import uk.gov.hmrc.auth.core.retrieve.Credentials

object TdApplicationData:

  @SuppressWarnings(Array("org.wartremover.warts.SeqApply"))
  def make(seed: String): ApplicationData =
    val random: scala.util.Random = new scala.util.Random(seed.hashCode)
    ApplicationData(
      applicationReference = ApplicationReference(s"APPREF_$seed"),
      internalUserId = InternalUserId(s"INTERNAL_USER_ID_$seed"),
      applicantCredentials = Credentials(providerId = s"providerid_$seed", providerType = s"providertype_$seed"),
      businessType = BusinessType.values(random.nextInt(BusinessType.values.size - 1)),
      groupId = GroupId(s"groupid_$seed"),
      applicantContactDetails = ApplicantContactDetailsData(
        applicantName = ApplicantName(s"applicantname_$seed"),
        telephoneNumber = TelephoneNumber(s"01234567890"),
        applicantEmailAddress = EmailAddress(s"applicantemail@$seed.com")
      ),
      amlsDetails = AmlsDetailsData(
        supervisoryBody = AmlsCode(s"amlscode_$seed"),
        amlsRegistrationNumber = AmlsRegistrationNumber(s"amlsregistrationnumber_$seed"),
        amlsEvidence = Some(AmlsEvidenceData(
          fileUploadReference = FileUploadReference(s"amls_fileupload_ref$seed"),
          fileName = s"amls_evicence_$seed"
        ))
      ),
      agentDetails = AgentDetailsData(
        businessName = AgentBusinessName(
          agentBusinessName = s"agentBusinessName_$seed",
          otherAgentBusinessName = Some(s"otherAgentBusinessName_$seed")
        ),
        telephoneNumber = AgentTelephoneNumber(
          agentTelephoneNumber = s"agentTelephoneNumber_$seed",
          otherAgentTelephoneNumber = Some(s"otherAgentTelephoneNumber_$seed")
        ),
        agentEmailAddress = AgentEmailAddress(
          agentEmailAddress = s"agentemail@$seed.com",
          otherAgentEmailAddress = Some(s"otheragentemail@$seed.com")
        ),
        agentCorrespondenceAddress = AgentCorrespondenceAddress(
          addressLine1 = s"addressline1_$seed",
          addressLine2 = Some(s"addressline2_$seed"),
          postalCode = Some(s"AB1 2CD"),
          countryCode = "GB"
        )
      ),
      deceasedCheckResult = CheckResult.Pass,
      vrns = List(Vrn(s"vrn_$seed")),
      payeRefs = List(PayeRef(s"payeref_$seed")),
      crn = Some(Crn(s"crn_$seed")),
      utr = Utr(s"utr_$seed"),
      safeId = SafeId(s"safeid_$seed")
    )
