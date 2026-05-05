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

package uk.gov.hmrc.agentregistrationrisking.controllers

import play.api.mvc.Call
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.risking.RiskingProgress
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.AuthStubs

class RiskingProgressControllerForApplicantSpec
extends ControllerSpec:

  val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]
  val agentApplicationReference: ApplicationReference = tdAll.tdRisking.agentApplication.applicationReference
  val path: String = s"/agent-registration-risking/risking-progress/for-applicant"

  "routes should have correct paths and methods" in:
    val call: Call = routes.RiskingProgressController.getRiskingProgressForApplicant(agentApplicationReference)
    call shouldBe Call(
      method = "GET",
      url = s"$path/${agentApplicationReference.value}"
    )

  "get application returns NO_CONTENT if there is no underlying records" in:
    given Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthorise()

    applicationForRiskingRepo.findById(agentApplicationReference).futureValue shouldBe None withClue " no prior records in mongo for this application"
    individualForRiskingRepo.findByApplicationReference(
      agentApplicationReference
    ).futureValue shouldBe Seq.empty withClue " no prior records in mongo for this application"

    val response: HttpResponse =
      httpClient
        .get(url"${baseUrl + path}/${agentApplicationReference.value}")
        .execute[HttpResponse]
        .futureValue

    response.status shouldBe Status.NO_CONTENT
    response.body shouldBe ""
    AuthStubs.verifyAuthorise()

//  "find application returns Ok and the Application as Json body" in:
//    given Request[?] = tdAll.backendRequest
//    AuthStubs.stubAuthorise()
//
//    val application = tdAll.llpApplicationForRisking
//    applicationForRiskingRepo.upsert(application).futureValue
//
//    val response =
//      httpClient
//        .get(url"$baseUrl/agent-registration-risking/application/${application.agentApplication.applicationReference.value}")
//        .execute[HttpResponse]
//        .futureValue
//    response.status shouldBe Status.OK
//    val parsedResponse = response.json.as[RiskingProgress]
//    parsedResponse.applicationReference shouldBe application.agentApplication.applicationReference
//    parsedResponse.status shouldBe RiskingStatus.ReadyForSubmission
//    parsedResponse.isSubscribed shouldBe false
//    parsedResponse.failures shouldBe None
//    parsedResponse.individuals shouldBe List.empty
//    AuthStubs.verifyAuthorise()
//
//  "find application with individuals returns Ok with individuals in response" in:
//    given Request[?] = tdAll.backendRequest
//    AuthStubs.stubAuthorise()
//
//    val application = tdAll.llpApplicationForRisking.copy(_id = ApplicationForRiskingId("test-app-with-individuals"))
//    applicationForRiskingRepo.upsert(application).futureValue
//
//    val individual = tdAll.readyForSubmissionIndividual(application._id)
//    individualForRiskingRepo.upsert(individual).futureValue
//
//    val response =
//      httpClient
//        .get(url"$baseUrl/agent-registration-risking/application/${application.agentApplication.applicationReference.value}")
//        .execute[HttpResponse]
//        .futureValue
//    response.status shouldBe Status.OK
//    val parsedResponse = response.json.as[RiskingProgress]
//    parsedResponse.applicationReference shouldBe application.agentApplication.applicationReference
//    parsedResponse.individuals.size shouldBe 1
//    parsedResponse.individuals.headOption.value.personReference shouldBe tdAll.personReference
//    parsedResponse.individuals.headOption.value.failures shouldBe None
//    AuthStubs.verifyAuthorise()
