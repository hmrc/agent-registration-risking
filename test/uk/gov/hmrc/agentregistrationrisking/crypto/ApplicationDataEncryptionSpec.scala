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

package uk.gov.hmrc.agentregistrationrisking.crypto

import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicationData
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates

class ApplicationDataEncryptionSpec
extends ISpec:

  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "field-level-encryption.enabled" -> true
  )

  private val fieldLevelEncryption: FieldLevelEncryption = app.injector.instanceOf[FieldLevelEncryption]
  private val applicationDataEncryption: ApplicationDataEncryption = app.injector.instanceOf[ApplicationDataEncryption]

  private def encrypt(plain: String): String = fieldLevelEncryption.encrypt(plain)

  private val applicationData: ApplicationData = TdRiskingInstancesInStates.approved.application.applicationData
  private val encrypted: ApplicationData = applicationDataEncryption.encrypt(applicationData)

  "ApplicationDataEncryption.encrypt sets encrypted on every PII field" - {

    "internalUserId is encrypted" in:
      encrypted.internalUserId.value shouldBe encrypt(applicationData.internalUserId.value)

    "applicantCredentials.providerId is encrypted" in:
      encrypted.applicantCredentials.providerId shouldBe encrypt(applicationData.applicantCredentials.providerId)

    "groupId is encrypted" in:
      encrypted.groupId.value shouldBe encrypt(applicationData.groupId.value)

    "applicantContactDetails.applicantName is encrypted" in:
      encrypted.applicantContactDetails.applicantName.value shouldBe encrypt(applicationData.applicantContactDetails.applicantName.value)

    "applicantContactDetails.telephoneNumber is encrypted" in:
      encrypted.applicantContactDetails.telephoneNumber.value shouldBe encrypt(applicationData.applicantContactDetails.telephoneNumber.value)

    "applicantContactDetails.applicantEmailAddress is encrypted" in:
      encrypted.applicantContactDetails.applicantEmailAddress.value shouldBe encrypt(applicationData.applicantContactDetails.applicantEmailAddress.value)

    "amlsDetails.amlsRegistrationNumber is encrypted" in:
      encrypted.amlsDetails.amlsRegistrationNumber.value shouldBe encrypt(applicationData.amlsDetails.amlsRegistrationNumber.value)

    "amlsDetails.amlsEvidence.fileName is encrypted" in:
      encrypted.amlsDetails.amlsEvidence.value.fileName shouldBe encrypt(applicationData.amlsDetails.amlsEvidence.value.fileName)

    "agentDetails.businessName.agentBusinessName is encrypted" in:
      encrypted.agentDetails.businessName.agentBusinessName shouldBe encrypt(applicationData.agentDetails.businessName.agentBusinessName)

    "agentDetails.businessName.otherAgentBusinessName is encrypted" in:
      encrypted.agentDetails.businessName.otherAgentBusinessName.value shouldBe encrypt(applicationData.agentDetails.businessName.otherAgentBusinessName.value)

    "agentDetails.telephoneNumber.agentTelephoneNumber is encrypted" in:
      encrypted.agentDetails.telephoneNumber.agentTelephoneNumber shouldBe encrypt(applicationData.agentDetails.telephoneNumber.agentTelephoneNumber)

    "agentDetails.telephoneNumber.otherAgentTelephoneNumber is encrypted" in:
      encrypted.agentDetails.telephoneNumber.otherAgentTelephoneNumber.value shouldBe encrypt(
        applicationData.agentDetails.telephoneNumber.otherAgentTelephoneNumber.value
      )

    "agentDetails.agentEmailAddress is encrypted" in:
      encrypted.agentDetails.agentEmailAddress.value shouldBe encrypt(applicationData.agentDetails.agentEmailAddress.value)

    "agentDetails.agentCorrespondenceAddress.addressLine1 is encrypted" in:
      encrypted.agentDetails.agentCorrespondenceAddress.addressLine1 shouldBe encrypt(applicationData.agentDetails.agentCorrespondenceAddress.addressLine1)

    "agentDetails.agentCorrespondenceAddress.addressLine2 is encrypted" in:
      encrypted.agentDetails.agentCorrespondenceAddress.addressLine2.value shouldBe encrypt(
        applicationData.agentDetails.agentCorrespondenceAddress.addressLine2.value
      )

    "agentDetails.agentCorrespondenceAddress.postalCode is encrypted" in:
      encrypted.agentDetails.agentCorrespondenceAddress.postalCode.value shouldBe encrypt(
        applicationData.agentDetails.agentCorrespondenceAddress.postalCode.value
      )

    "vrns are encrypted element-wise" in:
      encrypted.vrns.map(_.value) shouldBe applicationData.vrns.map(v => encrypt(v.value))

    "payeRefs are encrypted element-wise" in:
      encrypted.payeRefs.map(_.value) shouldBe applicationData.payeRefs.map(p => encrypt(p.value))

    "crn is encrypted" in:
      encrypted.crn.value.value shouldBe encrypt(applicationData.crn.value.value)

    "utr is encrypted" in:
      encrypted.utr.value shouldBe encrypt(applicationData.utr.value)

    "safeId is encrypted" in:
      encrypted.safeId.value shouldBe encrypt(applicationData.safeId.value)
  }

  "ApplicationDataEncryption.encrypt leaves non-PII / search-key fields untouched" - {

    "applicationReference stays plaintext (search key)" in:
      encrypted.applicationReference shouldBe applicationData.applicationReference

    "businessType stays plaintext (enum)" in:
      encrypted.businessType shouldBe applicationData.businessType

    "applicantCredentials.providerType stays plaintext" in:
      encrypted.applicantCredentials.providerType shouldBe applicationData.applicantCredentials.providerType

    "amlsDetails.supervisoryBody stays plaintext" in:
      encrypted.amlsDetails.supervisoryBody shouldBe applicationData.amlsDetails.supervisoryBody

    "amlsDetails.amlsEvidence.fileUploadReference stays plaintext (opaque id)" in:
      encrypted.amlsDetails.amlsEvidence.value.fileUploadReference shouldBe applicationData.amlsDetails.amlsEvidence.value.fileUploadReference

    "agentDetails.agentCorrespondenceAddress.countryCode stays plaintext" in:
      encrypted.agentDetails.agentCorrespondenceAddress.countryCode shouldBe applicationData.agentDetails.agentCorrespondenceAddress.countryCode
  }

  "ApplicationDataEncryption round-trips and does not leak plaintext PII" - {

    "decrypt(encrypt(x)) == x" in:
      applicationDataEncryption.decrypt(applicationDataEncryption.encrypt(applicationData)) shouldBe applicationData

    "rendered JSON of the encrypted applicationData contains no plaintext PII" in:
      val rendered = Json.toJson(encrypted).toString
      val correspondence = applicationData.agentDetails.agentCorrespondenceAddress
      val plaintextPii: List[String] =
        List(
          applicationData.internalUserId.value,
          applicationData.applicantCredentials.providerId,
          applicationData.groupId.value,
          applicationData.applicantContactDetails.applicantName.value,
          applicationData.applicantContactDetails.telephoneNumber.value,
          applicationData.applicantContactDetails.applicantEmailAddress.value,
          applicationData.amlsDetails.amlsRegistrationNumber.value,
          applicationData.amlsDetails.amlsEvidence.value.fileName,
          applicationData.agentDetails.businessName.agentBusinessName,
          applicationData.agentDetails.telephoneNumber.agentTelephoneNumber,
          applicationData.agentDetails.agentEmailAddress.value,
          correspondence.addressLine1,
          applicationData.utr.value,
          applicationData.safeId.value
        ) ++
          applicationData.agentDetails.businessName.otherAgentBusinessName.toList ++
          applicationData.agentDetails.telephoneNumber.otherAgentTelephoneNumber.toList ++
          correspondence.addressLine2.toList ++
          correspondence.addressLine3.toList ++
          correspondence.addressLine4.toList ++
          correspondence.postalCode.toList ++
          applicationData.vrns.map(_.value) ++
          applicationData.payeRefs.map(_.value) ++
          applicationData.crn.map(_.value).toList

      plaintextPii.foreach: plaintext =>
        withClue(s"plaintext '$plaintext' must not appear as a JSON value in the encrypted JSON: "):
          rendered should not include s"\"$plaintext\""
  }
