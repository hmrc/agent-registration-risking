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
import uk.gov.hmrc.agentregistration.shared.risking.IndividualRiskingResponse
import uk.gov.hmrc.agentregistration.shared.risking.PersonReference
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.AuthStubs

class GetIndividualControllerSpec /*
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

  val personReference: PersonReference = PersonReference(tdAll.personReference.value)
  val path: String = s"/agent-registration-risking/individual/${personReference.value}"

  "routes should have correct paths and methods" in:
    val call: Call = routes.GetIndividualController.getIndividualRiskingResponse(personReference)
    call shouldBe Call(
      method = "GET",
      url = path
    )

  "get individual returns NO_CONTENT if there is no underlying records" in:
    given Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthorise()
    val response =
      httpClient
        .get(url"$baseUrl/agent-registration-risking/individual/${tdAll.personReference.value}")
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.NO_CONTENT
    response.body shouldBe ""
    AuthStubs.verifyAuthorise()

  "find individual returns Ok and the Individual as Json body" in:
    given Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthorise()
    val repo = app.injector.instanceOf[ApplicationForRiskingRepo]
    val application = tdAll.llpApplicationForRisking
    repo.upsert(application).futureValue

    val response =
      httpClient
        .get(url"$baseUrl/agent-registration-risking/individual/${personReference.value}")
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.OK
    val parsedResponse = response.json.as[IndividualRiskingResponse]
    parsedResponse shouldBe tdAll.individualRiskingResponseReadyForSubmission(
      personReference = personReference
    )
    AuthStubs.verifyAuthorise()
