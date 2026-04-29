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
import uk.gov.hmrc.agentregistration.shared.risking.RiskingProgressForApplicant
import uk.gov.hmrc.agentregistration.shared.risking.RiskingStatus
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRiskingId
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.AuthStubs

class RiskingProgressForApplicantControllerSpec
extends ControllerSpec:

  val repo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  val individualRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]
  val agentApplicationReference: ApplicationReference = tdAll.applicationReference
  val path: String = s"/agent-registration-risking/application/${agentApplicationReference.value}"

  "routes should have correct paths and methods" in:
    val call: Call = routes.GetApplicationController.getApplicationRiskingResponse(agentApplicationReference)
    call shouldBe Call(
      method = "GET",
      url = path
    )

  "get application returns NO_CONTENT if there is no underlying records" in:
    given Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthorise()
    val response =
      httpClient
        .get(url"$baseUrl/agent-registration-risking/application/${agentApplicationReference.value}")
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.NO_CONTENT
    response.body shouldBe ""
    AuthStubs.verifyAuthorise()

  "find application returns Ok and the Application as Json body" in:
    given Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthorise()

    val application = tdAll.llpApplicationForRisking
    repo.upsert(application).futureValue

    val response =
      httpClient
        .get(url"$baseUrl/agent-registration-risking/application/${application.agentApplication.applicationReference.value}")
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.OK
    val parsedResponse = response.json.as[RiskingProgressForApplicant]
    parsedResponse.applicationReference shouldBe application.agentApplication.applicationReference
    parsedResponse.status shouldBe RiskingStatus.ReadyForSubmission
    parsedResponse.isSubscribed shouldBe false
    parsedResponse.failures shouldBe None
    parsedResponse.individuals shouldBe List.empty
    AuthStubs.verifyAuthorise()

  "find application with individuals returns Ok with individuals in response" in:
    given Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthorise()

    val application = tdAll.llpApplicationForRisking.copy(_id = ApplicationForRiskingId("test-app-with-individuals"))
    repo.upsert(application).futureValue

    val individual = tdAll.readyForSubmissionIndividual(application._id)
    individualRepo.upsert(individual).futureValue

    val response =
      httpClient
        .get(url"$baseUrl/agent-registration-risking/application/${application.agentApplication.applicationReference.value}")
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.OK
    val parsedResponse = response.json.as[RiskingProgressForApplicant]
    parsedResponse.applicationReference shouldBe application.agentApplication.applicationReference
    parsedResponse.individuals.size shouldBe 1
    parsedResponse.individuals.headOption.value.personReference shouldBe tdAll.personReference
    parsedResponse.individuals.headOption.value.failures shouldBe None
    AuthStubs.verifyAuthorise()
