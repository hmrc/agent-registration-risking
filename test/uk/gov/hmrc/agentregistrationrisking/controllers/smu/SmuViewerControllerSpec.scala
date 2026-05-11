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

///*
// * Copyright 2025 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package uk.gov.hmrc.agentregistrationrisking.controllers.smu
//
//import org.scalactic.Prettifier.default
//import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
//import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
//import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
//import uk.gov.hmrc.agentregistrationrisking.testsupport.ControllerSpec
//import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.AuthStubs
//import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.ObjectStoreStubs
//import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging
//
//class SmuViewerControllerSpec
//extends ControllerSpec
//with RequestAwareLogging:
//
//  "find individual by person reference returns Ok and SmuIndividualResponse as Json body" in:
//
//    given Request[?] = tdAll.backendRequest
//    AuthStubs.stubAuthorise()
//    val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]
//    val individualForRisking: IndividualForRisking = tdAll.readyForSubmissionIndividual()
//    individualForRiskingRepo.upsert(individualForRisking).futureValue
//    val applicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
//    val applicationForRisking = tdAll.llpApplicationForRisking.copy(_id = individualForRisking.applicationForRiskingId)
//    applicationForRiskingRepo.upsert(applicationForRisking).futureValue
//
//    individualForRiskingRepo.findByPersonReference(
//      individualForRisking.individualProvidedDetails.personReference
//    ).futureValue.value shouldBe individualForRisking withClue "sanity check"
//
//    applicationForRiskingRepo.findById(
//      applicationForRisking._id
//    ).futureValue.value shouldBe applicationForRisking withClue "sanity check"
//
//    ObjectStoreStubs.stubObjectStoreGeneratePresignedUrl
//
//    val response: HttpResponse =
//      httpClient
//        .get(
//          url"$baseUrl/agent-registration-risking/smu-viewer/individual/by-person-reference/${individualForRisking.individualProvidedDetails.personReference.value}"
//        )
//        .execute[HttpResponse]
//        .futureValue
//
//    response.status shouldBe Status.OK
//    response.json shouldBe
//      Json.parse(
//        s"""{
//           |  "individual" : {
//           |    "personReference" : "1234567890",
//           |    "resubmission" : false,
//           |    "passedIdentityVerification" : true,
//           |    "detailsProvidedByApplicant" : false,
//           |    "individualName" : "Test Name",
//           |    "individualDateOfBirth" : "1980-01-01",
//           |    "individualNino" : "AB123456C",
//           |    "individualSaUtr" : "1234567895",
//           |    "payeRefs":["123/AB12345","123/AB12345"],
//           |    "vrns":["123456789","123456789"],
//           |    "telephoneNumber" : "(+44) 10794554342",
//           |    "emailAddress" : "member@test.com"
//           |  },
//           |  "entity" : {
//           |    "applicationReference" : "ABC123456",
//           |    "resubmission" : false,
//           |    "applicantName" : "Alice Smith",
//           |    "businessType" : "LimitedLiabilityPartnership",
//           |    "utr" : "1234567895",
//           |    "payeRefs" : [ "123/AB12345", "123/AB12345" ],
//           |    "vrns" : [ "123456789", "123456789" ],
//           |    "crn" : "1234567890",
//           |    "amlsSupervisoryBody" : "HMRC",
//           |    "amlsRegNumber" : "XAML00000123456",
//           |    "amlsEvidencePresignedDownloadUrl" : "http://presigned-url/file",
//           |    "applicantPhone" : "(+44) 10794554342",
//           |    "applicantEmail" : "user@test.com"
//           |  }
//           |} """.stripMargin
//      )
