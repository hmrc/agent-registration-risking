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
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.risking.SubmitForRiskingRequest
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.AuthStubs

import scala.concurrent.Future

class GetApplicationStatusControllerSpec
extends ControllerSpec:

  val agentApplicationReference: ApplicationReference = ApplicationReference(tdAll.agentApplicationId.value)
  val path: String = s"/agent-registration-risking/application-status/${agentApplicationReference.value}"

  "routes should have correct paths and methods" in:
    val call: Call = routes.GetApplicationStatusController.getApplicationStatus(agentApplicationReference)
    call shouldBe Call(
      method = "GET",
      url = path
    )

  "get application status returns OK with status in response" in:
    given Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthorise()
    val repo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]

    repo.upsert(
      (tdAll.llpApplicationForRisking)
    ).futureValue shouldBe () withClue "ensure there is an application for risking in mongo before http request"

    val response =
      httpClient
        .get(url"$baseUrl/agent-registration-risking/application-status/${tdAll.llpApplicationForRisking.applicationReference.value}")
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.OK
    response.body shouldBe Json.obj(
      "status" -> tdAll.llpApplicationForRisking.status.toString
    ).toString() withClue "response should contain the status of the application for risking"
    AuthStubs.verifyAuthorise()
