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
//package uk.gov.hmrc.agentregistrationrisking.controllers
//
//import play.api.mvc.Call
//import uk.gov.hmrc.agentregistration.shared.risking.RiskingStatus
//import uk.gov.hmrc.agentregistration.shared.risking.SubmitForRiskingRequest
//import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
//import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
//import uk.gov.hmrc.agentregistrationrisking.testsupport.ControllerSpec
//import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.AuthStubs
//
//class SubmitForRiskingControllerSpec
//extends ControllerSpec:
//
//  val repo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
//  val individualRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]
//  val path: String = s"/agent-registration-risking/submit-for-risking"
//
//  "routes should have correct paths and methods" in:
//    val call: Call = routes.SubmitForRiskingController.submitForRisking()
//    call shouldBe Call(
//      method = "POST",
//      url = path
//    )
//
//  "upsert application upserts application and individuals to mongo and returns CREATED" in:
//    given Request[?] = tdAll.backendRequest
//    AuthStubs.stubAuthorise()
//
//    val submitRequest = SubmitForRiskingRequest(
//      tdAll.agentApplicationLlp.afterSentForRisking,
//      List(tdAll.providedDetails.afterFinished)
//    )
//
//    val applicationReference = submitRequest.agentApplication.applicationReference
//
//    repo.findByApplicationReference(applicationReference).futureValue shouldBe None withClue
//      "assuming initially there are no records in mongo"
//
//    val response =
//      httpClient
//        .post(url"$baseUrl/agent-registration-risking/submit-for-risking")
//        .withBody(Json.toJson(submitRequest))
//        .execute[HttpResponse]
//        .futureValue
//    response.status shouldBe Status.CREATED
//    response.body shouldBe ""
//
//    val savedApplication = repo.findByApplicationReference(applicationReference).futureValue.value
//    savedApplication.agentApplication.applicationReference shouldBe applicationReference
//    savedApplication.status shouldBe RiskingStatus.ReadyForSubmission
//    savedApplication.failures shouldBe None
//    savedApplication.isSubscribed shouldBe false
//    savedApplication.isEmailSent shouldBe false
//
//    val savedIndividuals = individualRepo.findByApplicationForRiskingId(savedApplication._id).futureValue
//    savedIndividuals.size shouldBe 1
//    savedIndividuals.headOption.value.individualProvidedDetails.personReference shouldBe tdAll.personReference
//    savedIndividuals.headOption.value.failures shouldBe None
//
//    AuthStubs.verifyAuthorise()
