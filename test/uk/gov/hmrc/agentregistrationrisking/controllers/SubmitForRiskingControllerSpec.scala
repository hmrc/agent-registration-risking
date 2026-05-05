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
import uk.gov.hmrc.agentregistration.shared.risking.SubmitForRiskingRequest
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.AuthStubs

class SubmitForRiskingControllerSpec
extends ControllerSpec:

  val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]
  val path: String = s"/agent-registration-risking/submit-for-risking"

  "routes should have correct paths and methods" in:
    val call: Call = routes.SubmitForRiskingController.submitForRisking()
    call shouldBe Call(
      method = "POST",
      url = path
    )

  "submit application and individuals for risking for the first time" in:
    // GIVEN
    given Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthorise()

    val submitRequest: SubmitForRiskingRequest = tdAll.tdRisking.submitForRiskingRequest
    val applicationReference: ApplicationReference = submitRequest.agentApplication.applicationReference

    val applicationForRiskingSubmitted: ApplicationForRisking = tdAll.tdRisking.tdApplicationForRisking.submitted
    val individualForRiskingSubmitted1: IndividualForRisking = tdAll.tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.submitted
    val individualForRiskingSubmitted2: IndividualForRisking = tdAll.tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.submitted

    applicationForRiskingRepo
      .findById(applicationReference)
      .futureValue shouldBe None withClue " no prior records in mongo for this application"
    individualForRiskingRepo
      .findByApplicationReference(applicationReference)
      .futureValue shouldBe Seq.empty withClue " no prior records in mongo for this application"

    // WHEN
    val response =
      httpClient
        .post(url"${baseUrl + path}")
        .withBody(Json.toJson(submitRequest))
        .execute[HttpResponse]
        .futureValue

    // THEN
    response.status shouldBe Status.CREATED
    response.body shouldBe ""

    applicationForRiskingRepo
      .findById(applicationReference)
      .futureValue
      .value shouldBe applicationForRiskingSubmitted withClue " no prior records in mongo for this application"

    individualForRiskingRepo
      .findByApplicationReference(applicationReference)
      .futureValue shouldBe List(
      individualForRiskingSubmitted1,
      individualForRiskingSubmitted2
    )

    AuthStubs.verifyAuthorise()
