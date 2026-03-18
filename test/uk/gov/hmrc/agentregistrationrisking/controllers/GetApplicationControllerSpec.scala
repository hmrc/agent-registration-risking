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
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationRiskingResponse
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.AuthStubs

class GetApplicationControllerSpec /*
 * Copyright 2025 HM Revenue & Customs
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
extends ControllerSpec:

  val agentApplicationReference: ApplicationReference = ApplicationReference(tdAll.agentApplicationId.value)
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
        .get(url"$baseUrl/agent-registration-risking/application/${tdAll.agentApplicationId.value}")
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.NO_CONTENT
    response.body shouldBe ""
    AuthStubs.verifyAuthorise()

  "find application returns Ok and the Application as Json body" in:
    given Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthorise()
    val repo = app.injector.instanceOf[ApplicationForRiskingRepo]
    val application = tdAll.llpApplicationForRisking
    repo.upsert(application).futureValue

    val response =
      httpClient
        .get(url"$baseUrl/agent-registration-risking/application/${application.applicationReference.value}")
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.OK
    val parsedResponse = response.json.as[ApplicationRiskingResponse]
    parsedResponse shouldBe tdAll.applicationRiskingResponseReadyForSubmission(
      applicationReference = application.applicationReference,
      personReference = tdAll.personReference
    )
    AuthStubs.verifyAuthorise()
