///*
// * Copyright 2026 HM Revenue & Customs
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
//import uk.gov.hmrc.agentregistration.shared.AmlsCode
//import uk.gov.hmrc.agentregistration.shared.AmlsDetails
//import uk.gov.hmrc.agentregistration.shared.AmlsRegistrationNumber
//import uk.gov.hmrc.agentregistration.shared.amls.AmlsEvidence
//import uk.gov.hmrc.agentregistration.shared.upload.FileUploadReference
//import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
//import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
//import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
//import uk.gov.hmrc.agentregistrationrisking.testsupport.ControllerSpec
//import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.tdRisking
//import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.AuthStubs
//import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging
//import uk.gov.hmrc.objectstore.client.Path.File
//
//class SmuViewerControllerSpec
//extends ControllerSpec
//with RequestAwareLogging:
//  "find individual by person reference returns Ok and SmuIndividualResponse as Json body" in:
//    given Request[?] = tdAll.backendRequest
//    AuthStubs.stubAuthorise()
//    val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]
//    val individualForRisking: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.readyForSubmission
//    individualForRiskingRepo.upsert(individualForRisking).futureValue
//    val applicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
//    val agentApplication = tdRisking.tdApplicationForRisking.submittedForRisking.agentApplication.asLlpApplication.copy(amlsDetails =
//      Some(AmlsDetails(
//        supervisoryBody = AmlsCode("ACCA"),
//        amlsRegistrationNumber = Some(AmlsRegistrationNumber("12345")),
//        amlsEvidence = Some(AmlsEvidence(
//          fileUploadReference = FileUploadReference("send-me-12345"),
//          fileName = "amls-evidence-file-123",
//          objectStoreLocation = File("uri")
//        ))
//      ))
//    )
//    val applicationForRisking = tdRisking.tdApplicationForRisking.submittedForRisking.copy(
//      applicationReference = individualForRisking.applicationReference,
//      agentApplication = agentApplication
//    )
//    applicationForRiskingRepo.upsert(applicationForRisking).futureValue
//
//    individualForRiskingRepo.findById(
//      individualForRisking.individualProvidedDetails.personReference
//    ).futureValue.value shouldBe individualForRisking withClue "sanity check"
//
//    applicationForRiskingRepo.findById(
//      applicationForRisking.applicationReference
//    ).futureValue.value shouldBe applicationForRisking withClue "sanity check"
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
//           |"individual":{
//           |  "personReference":"PREFGENP02",
//           |  "resubmission":false,
//           |  "passedIdentityVerification":true,
//           |  "detailsProvidedByApplicant":false,
//           |  "individualName":"Test Name",
//           |  "individualDateOfBirth":"2000-01-01",
//           |  "individualNino":"AB123456C",
//           |  "individualSaUtr":"1234567895",
//           |  "payeRefs":["123/AB12345"],
//           |  "vrns":["123456789"],
//           |  "telephoneNumber":"(+44) 10794554342",
//           |  "emailAddress":"member@test.com"
//           |},
//           |"entity":{
//           |  "applicationReference":"APPGENPAR1",
//           |  "resubmission":false,
//           |  "applicantName":"Alice Smith",
//           |  "businessType":"GeneralPartnership",
//           |  "utr":"1234567895",
//           |  "amlsSupervisoryBody":"HMRC",
//           |  "amlsRegNumber":"XAML00000123456",
//           |  "applicantPhone":"(+44) 10794554342",
//           |  "applicantEmail":"user@test.com"
//           |  }
//           |} """.stripMargin
//      )
