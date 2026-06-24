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

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistrationrisking.testsupport.UnitSpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdCompletedRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRisking

class CompletedRiskingSpec
extends UnitSpec:

  "serialize/deserialize" in:
    Json.toJson(Fixture.completedRisking) shouldBe Fixture.completedRiskingApprovedJson
    Fixture.completedRiskingApprovedJson.as[CompletedRisking] shouldBe Fixture.completedRisking

  object Fixture:

    val tdRisking: TdRisking = TdRisking.make("CompletedRiskingSpec")
    val completedRisking: CompletedRisking = TdCompletedRisking.makeCompletedRisking(
      completedRiskingId = CompletedRiskingId("CR_123"),
      completedAt = tdRisking.instant,
      riskingFile = tdRisking.riskingFile,
      application =
        tdRisking
          .tdApplicationForRisking
          .receivedRiskingResults
          .failedNonFixableAfterEmailsProcessed,
      individuals = Seq(
        tdRisking
          .tdIndividualsForRisking
          .tdIndividualForRisking1
          .receivedRiskingResults
          .failedNonFixableEmailSent,
        tdRisking
          .tdIndividualsForRisking
          .tdIndividualForRisking2
          .receivedRiskingResults
          .failedFixable
      )
    )

    val completedRiskingApprovedJson: JsValue = Json.parse(
      // language=JSON
      """{
        |  "_id": "CR_123",
        |  "completedAt": "2059-11-25T16:33:51Z",
        |  "riskingFile": {
        |    "riskingFileName": "asa_risking_file_version1_0_4_20591125_163351.txt",
        |    "uploadedAt": "2059-11-25T16:33:51Z"
        |  },
        |  "application": {
        |    "applicationReference": "APPREF_CompletedRiskingSpec",
        |    "riskingFileName": "asa_risking_file_version1_0_4_20591125_163351.txt",
        |    "applicationData": {
        |      "applicationReference": "APPREF_CompletedRiskingSpec",
        |      "internalUserId": "INTERNAL_USER_ID_CompletedRiskingSpec",
        |      "applicantCredentials": {
        |        "providerId": "providerid_CompletedRiskingSpec",
        |        "providerType": "providertype_CompletedRiskingSpec"
        |      },
        |      "businessType": "ScottishLimitedPartnership",
        |      "groupId": "groupid_CompletedRiskingSpec",
        |      "applicantContactDetails": {
        |        "applicantName": "applicantname_CompletedRiskingSpec",
        |        "telephoneNumber": "01234567890",
        |        "applicantEmailAddress": "applicantemail@CompletedRiskingSpec.com"
        |      },
        |      "amlsDetails": {
        |        "supervisoryBody": "amlscode_CompletedRiskingSpec",
        |        "amlsRegistrationNumber": "amlsregistrationnumber_CompletedRiskingSpec",
        |        "amlsEvidence": {
        |          "fileUploadReference": "amls_fileupload_refCompletedRiskingSpec",
        |          "fileName": "amls_evicence_CompletedRiskingSpec"
        |        }
        |      },
        |      "agentDetails": {
        |        "businessName": {
        |          "agentBusinessName": "agentBusinessName_CompletedRiskingSpec",
        |          "otherAgentBusinessName": "otherAgentBusinessName_CompletedRiskingSpec"
        |        },
        |        "telephoneNumber": {
        |          "agentTelephoneNumber": "agentTelephoneNumber_CompletedRiskingSpec",
        |          "otherAgentTelephoneNumber": "otherAgentTelephoneNumber_CompletedRiskingSpec"
        |        },
        |        "agentEmailAddress": "agentemail@CompletedRiskingSpec.com",
        |        "agentCorrespondenceAddress": {
        |          "addressLine1": "addressline1_CompletedRiskingSpec",
        |          "addressLine2": "addressline2_CompletedRiskingSpec",
        |          "postalCode": "AB1 2CD",
        |          "countryCode": "GB"
        |        }
        |      },
        |      "vrns": [
        |        "vrn_CompletedRiskingSpec"
        |      ],
        |      "payeRefs": [
        |        "payeref_CompletedRiskingSpec"
        |      ],
        |      "crn": "crn_CompletedRiskingSpec",
        |      "utr": "utr_CompletedRiskingSpec",
        |      "safeId": "safeid_CompletedRiskingSpec",
        |      "arn": "arn_CompletedRiskingSpec"
        |    },
        |    "createdAt": "2059-11-25T16:33:51Z",
        |    "lastUpdatedAt": "2059-11-25T16:33:51Z",
        |    "entityRiskingResult": {
        |      "failures": [
        |        {
        |          "type": "_3._2"
        |        },
        |        {
        |          "type": "_8._4"
        |        }
        |      ],
        |      "receivedAt": "2059-11-25T16:33:51Z"
        |    },
        |    "isSubscribed": false,
        |    "isEmailSent": true,
        |    "overallStatus": {
        |      "riskingOutcome": "FailedNonFixable",
        |      "emailsProcessed": true
        |    },
        |    "correctiveActionExpiryDate": "2060-01-09T16:33:51Z"
        |  },
        |  "individuals": [
        |    {
        |      "personReference": "PREF_CompletedRiskingSpec_01",
        |      "applicationReference": "APPREF_CompletedRiskingSpec",
        |      "individualData": {
        |        "personReference": "PREF_CompletedRiskingSpec_01",
        |        "individualName": "IndividualName_CompletedRiskingSpec_01",
        |        "isPersonOfControl": true,
        |        "individualDateOfBirth": {
        |          "dateOfBirth": "2009-01-01",
        |          "type": "Provided"
        |        },
        |        "telephoneNumber": "01234567-22",
        |        "emailAddress": "individual_email_CompletedRiskingSpec_01@test.com",
        |        "individualNino": {
        |          "nino": "AB123456C_CompletedRiskingSpec_01",
        |          "type": "Provided"
        |        },
        |        "individualSaUtr": {
        |          "saUtr": "1234567895_CompletedRiskingSpec_01",
        |          "type": "Provided"
        |        },
        |        "vrns": [
        |          "vrn_CompletedRiskingSpec_01"
        |        ],
        |        "payeRefs": [
        |          "payeref_CompletedRiskingSpec_01"
        |        ],
        |        "passedIv": false,
        |        "providedByApplicant": false
        |      },
        |      "createdAt": "2059-11-25T16:33:51Z",
        |      "lastUpdatedAt": "2059-11-25T16:33:51Z",
        |      "individualRiskingResult": {
        |        "failures": [
        |          {
        |            "type": "_4._3"
        |          },
        |          {
        |            "type": "_7"
        |          }
        |        ],
        |        "receivedAt": "2059-11-23T16:33:51Z"
        |      },
        |      "isEmailSent": true
        |    },
        |    {
        |      "personReference": "PREF_CompletedRiskingSpec_02",
        |      "applicationReference": "APPREF_CompletedRiskingSpec",
        |      "individualData": {
        |        "personReference": "PREF_CompletedRiskingSpec_02",
        |        "individualName": "IndividualName_CompletedRiskingSpec_02",
        |        "isPersonOfControl": false,
        |        "individualDateOfBirth": {
        |          "dateOfBirth": "2001-01-01",
        |          "type": "Provided"
        |        },
        |        "telephoneNumber": "01234567-753",
        |        "emailAddress": "individual_email_CompletedRiskingSpec_02@test.com",
        |        "individualNino": {
        |          "nino": "AB123456C_CompletedRiskingSpec_02",
        |          "type": "Provided"
        |        },
        |        "individualSaUtr": {
        |          "saUtr": "1234567895_CompletedRiskingSpec_02",
        |          "type": "Provided"
        |        },
        |        "vrns": [
        |          "vrn_CompletedRiskingSpec_02"
        |        ],
        |        "payeRefs": [
        |          "payeref_CompletedRiskingSpec_02"
        |        ],
        |        "passedIv": false,
        |        "providedByApplicant": false
        |      },
        |      "createdAt": "2059-11-25T16:33:51Z",
        |      "lastUpdatedAt": "2059-11-25T16:33:51Z",
        |      "individualRiskingResult": {
        |        "failures": [
        |          {
        |            "type": "_4._1"
        |          },
        |          {
        |            "type": "_4._3"
        |          }
        |        ],
        |        "receivedAt": "2059-11-23T16:33:51Z"
        |      },
        |      "isEmailSent": false
        |    }
        |  ]
        |} 
        |""".stripMargin
    )
