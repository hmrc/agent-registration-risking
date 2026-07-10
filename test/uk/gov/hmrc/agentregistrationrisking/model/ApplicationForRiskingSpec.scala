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

import com.softwaremill.quicklens.modify
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistrationrisking.testsupport.UnitSpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdApplicationData
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdInstant
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates

import java.time.Instant

class ApplicationForRiskingSpec
extends UnitSpec:

  "serialize and deserialize ApplicationForRisking with and without isResubmission" in:
    val applicationForRisking: ApplicationForRisking = ApplicationForRisking(
      applicationReference = ApplicationReference("APP-123"),
      riskingFileName = Some(RiskingFileName("some-file.txt")),
      applicationData = TdApplicationData.make("serialisation"),
      createdAt = TdInstant.instant,
      lastUpdatedAt = TdInstant.instant,
      isSubscribed = false,
      isEmailSent = false,
      overallStatus = OverallStatus(
        riskingOutcome = None,
        emailsProcessed = false,
        backendNotified = false,
        emailsSentAt = None
      ),
      entityRiskingResult = None,
      correctiveActionExpiryDate = Some(TdInstant.instant),
      isResubmission = false
    )
    val json: JsValue = Json.parse(
      // language=JSON
      """{
         "applicationReference": "APP-123",
         "riskingFileName": "some-file.txt",
         "applicationData": {
           "applicationReference": "APPREF_serialisation",
           "internalUserId": "INTERNAL_USER_ID_serialisation",
           "applicantCredentials": {
             "providerId": "providerid_serialisation",
             "providerType": "providertype_serialisation"
           },
           "businessType": "LimitedCompany",
           "groupId": "groupid_serialisation",
            "applicantContactDetails": {
              "applicantName": "applicantname_serialisation",
              "telephoneNumber": "01234567890",
              "applicantEmailAddress": "applicantemail@serialisation.com"
            },
            "amlsDetails": {
              "supervisoryBody": "amlscode_serialisation",
              "amlsRegistrationNumber": "amlsregistrationnumber_serialisation",
              "amlsEvidence": {
                "fileUploadReference": "amls_fileupload_refserialisation",
                "fileName": "amls_evicence_serialisation"
              }
            },
            "agentDetails": {
              "businessName": {
                "agentBusinessName": "agentBusinessName_serialisation",
                "otherAgentBusinessName": "otherAgentBusinessName_serialisation"
              },
              "telephoneNumber": {
                "agentTelephoneNumber": "agentTelephoneNumber_serialisation",
                 "otherAgentTelephoneNumber": "otherAgentTelephoneNumber_serialisation"
              },
              "agentEmailAddress": "agentemail@serialisation.com",
              "agentCorrespondenceAddress": {
                "addressLine1": "addressline1_serialisation",
                "addressLine2": "addressline2_serialisation",
                "postalCode": "AB1 2CD",
                "countryCode": "GB"
              }
            },
            "vrns": [
              "vrn_serialisation"
            ],
            "payeRefs": [
              "payeref_serialisation"
            ],
            "crn": "crn_serialisation",
            "utr": "utr_serialisation",
            "safeId": "safeid_serialisation",
            "arn": "arn_serialisation"
         },
         "createdAt": "2059-11-25T16:33:51Z",
         "lastUpdatedAt": "2059-11-25T16:33:51Z",
         "isSubscribed": false,
         "isEmailSent": false,
         "overallStatus": {
           "emailsProcessed": false,
           "backendNotified": false
         },
         "correctiveActionExpiryDate": "2059-11-25T16:33:51Z",
         "isResubmission": false
      }""".stripMargin
    )
    val legacyJson: JsValue = Json.parse(
      // language=JSON
      """{
         "applicationReference": "APP-123",
         "riskingFileName": "some-file.txt",
         "applicationData": {
           "applicationReference": "APPREF_serialisation",
           "internalUserId": "INTERNAL_USER_ID_serialisation",
           "applicantCredentials": {
             "providerId": "providerid_serialisation",
             "providerType": "providertype_serialisation"
           },
           "businessType": "LimitedCompany",
           "groupId": "groupid_serialisation",
            "applicantContactDetails": {
              "applicantName": "applicantname_serialisation",
              "telephoneNumber": "01234567890",
              "applicantEmailAddress": "applicantemail@serialisation.com"
            },
            "amlsDetails": {
              "supervisoryBody": "amlscode_serialisation",
              "amlsRegistrationNumber": "amlsregistrationnumber_serialisation",
              "amlsEvidence": {
                "fileUploadReference": "amls_fileupload_refserialisation",
                "fileName": "amls_evicence_serialisation"
              }
            },
            "agentDetails": {
              "businessName": {
                "agentBusinessName": "agentBusinessName_serialisation",
                "otherAgentBusinessName": "otherAgentBusinessName_serialisation"
              },
              "telephoneNumber": {
                "agentTelephoneNumber": "agentTelephoneNumber_serialisation",
                 "otherAgentTelephoneNumber": "otherAgentTelephoneNumber_serialisation"
              },
              "agentEmailAddress": "agentemail@serialisation.com",
              "agentCorrespondenceAddress": {
                "addressLine1": "addressline1_serialisation",
                "addressLine2": "addressline2_serialisation",
                "postalCode": "AB1 2CD",
                "countryCode": "GB"
              }
            },
            "vrns": [
              "vrn_serialisation"
            ],
            "payeRefs": [
              "payeref_serialisation"
            ],
            "crn": "crn_serialisation",
            "utr": "utr_serialisation",
            "safeId": "safeid_serialisation",
            "arn": "arn_serialisation"
         },
         "createdAt": "2059-11-25T16:33:51Z",
         "lastUpdatedAt": "2059-11-25T16:33:51Z",
         "isSubscribed": false,
         "isEmailSent": false,
         "overallStatus": {
           "emailsProcessed": false,
           "backendNotified": false
         },
         "correctiveActionExpiryDate": "2059-11-25T16:33:51Z"
      }""".stripMargin
    )
    /**
     * The legacyJson is missing the isResubmission field, which was added later. The reads should default it to false when reading legacy documents.
     */
    val readLegacy: ApplicationForRisking = legacyJson.as[ApplicationForRisking]
    readLegacy shouldBe applicationForRisking
    Json.toJson[ApplicationForRisking](applicationForRisking) shouldBe json
    json.as[ApplicationForRisking] shouldBe applicationForRisking

  "reads derives overallStatus.emailsSentAt from entityRiskingResult.receivedAt when a legacy document has emailsProcessed=true but no emailSentAt" in:
    val legacyApplicationForRisking: ApplicationForRisking = TdRiskingInstancesInStates
      .approvedAfterEmailSent
      .application
      .modify(_.overallStatus.emailsSentAt).setTo(None)
    val entityReceivedAt: Instant = legacyApplicationForRisking.entityRiskingResult.value.receivedAt

    val legacyJson = Json.toJson(legacyApplicationForRisking)
    val readBack: ApplicationForRisking = legacyJson.as[ApplicationForRisking]

    readBack.overallStatus.emailsSentAt shouldBe Some(entityReceivedAt)

  "reads does not overwrite an existing emailSentAt" in:
    val applicationForRisking: ApplicationForRisking = TdRiskingInstancesInStates.approvedAfterEmailSent.application
    val presetEmailSentAt: Instant = TdInstant.instant.plusSeconds(60)
    val applicationForRiskingWithEmailSentAt: ApplicationForRisking = applicationForRisking
      .modify(_.overallStatus.emailsSentAt).setTo(Some(presetEmailSentAt))

    val json = Json.toJson(applicationForRiskingWithEmailSentAt)
    val readBack: ApplicationForRisking = json.as[ApplicationForRisking]

    readBack.overallStatus.emailsSentAt shouldBe Some(presetEmailSentAt)

  "reads leaves emailSentAt as None when emailsProcessed=false" in:
    val applicationForRisking: ApplicationForRisking = TdRiskingInstancesInStates.approvedAfterOutcome.application

    val json = Json.toJson(applicationForRisking)
    val readBack: ApplicationForRisking = json.as[ApplicationForRisking]

    readBack.overallStatus.emailsProcessed shouldBe false
    readBack.overallStatus.emailsSentAt shouldBe None

  "reads leaves emailSentAt as None when emailsProcessed=true but entityRiskingResult is missing — impossible under the state ladder but the derivation must not crash on it" in:
    val impossibleApplicationForRisking: ApplicationForRisking = TdRiskingInstancesInStates
      .approvedAfterEmailSent
      .application
      .modify(_.overallStatus.emailsSentAt).setTo(None)
      .copy(entityRiskingResult = None)

    val json = Json.toJson(impossibleApplicationForRisking)
    val readBack: ApplicationForRisking = json.as[ApplicationForRisking]

    readBack.overallStatus.emailsProcessed shouldBe true
    readBack.overallStatus.emailsSentAt shouldBe None
