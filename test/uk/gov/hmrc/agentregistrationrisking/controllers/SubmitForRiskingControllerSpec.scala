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
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistrationrisking.model.SubmitForRiskingRequest
import uk.gov.hmrc.agentregistrationrisking.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.AuthStubs

class SubmitForRiskingControllerSpec
extends ControllerSpec:

  val agentApplicationId: AgentApplicationId = tdAll.agentApplicationId
  val path: String = s"/agent-registration-risking/submit-for-risking/${agentApplicationId.value}"

  "routes should have correct paths and methods" in:
    val call: Call = routes.SubmitForRiskingController.submitForRisking(agentApplicationId)
    call shouldBe Call(
      method = "POST",
      url = path
    )

  "submit for risking" in:

    given Request[?] = tdAll.backendRequest

    AuthStubs.stubAuthorise()

    val request = SubmitForRiskingRequest("todo")

    val response =
      httpClient
        .post(url"$baseUrl/agent-registration-risking/submit-for-risking/${agentApplicationId.value}")
        .withBody(Json.toJson(request))
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.ACCEPTED
    response.body shouldBe ""
