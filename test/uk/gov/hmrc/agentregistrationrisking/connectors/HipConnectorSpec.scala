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

package uk.gov.hmrc.agentregistrationrisking.connectors

import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.SafeId
import uk.gov.hmrc.agentregistration.shared.Arn
import uk.gov.hmrc.agentregistrationrisking.model.hip.SubscribeAgentRequest
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.HipStubs

class HipConnectorSpec
extends ISpec:

  val connector: HipConnector = app.injector.instanceOf[HipConnector]

  val safeId = SafeId("safe-1")

  implicit val request: Request[?] = FakeRequest()

  val arn = "HARN0001234"

  val subscribeAgentRequest = SubscribeAgentRequest(
    name = "Xyz",
    addr1 = "1 Any St",
    addr2 = "Any town",
    addr3 = None,
    addr4 = None,
    postcode = None,
    country = "GB",
    email = EmailAddress("a@b.com"),
    phone = None,
    supervisoryBody = None,
    membershipNumber = None,
    evidenceObjectReference = None,
    updateDetailsStatus = "REQUIRED",
    amlSupervisionUpdateStatus = "REQUIRED",
    directorPartnerUpdateStatus = "REQUIRED",
    acceptNewTermsStatus = "REQUIRED",
    reriskStatus = "REQUIRED"
  )

  "returns an ARN when the API response is 201 Created" in:
    HipStubs.stubSubscribeToAgentServices(safeId, arn)
    connector.subscribeToAgentServices(safeId, subscribeAgentRequest).futureValue shouldBe Arn(arn)
    HipStubs.verifySubscribeToAgentServices()

  "returns an ARN when the API response is 422 with code 061 BP has already a valid Agent Subscription" in:
    HipStubs.stubSubscribeToAgentServicesAlreadySubscribed(safeId, arn)
    connector.subscribeToAgentServices(safeId, subscribeAgentRequest).futureValue shouldBe Arn(arn)
    HipStubs.verifySubscribeToAgentServices()

  "throws an exception when the API response is 422 with code 061 and ARN not in expected place" in:
    HipStubs.stubSubscribeToAgentServicesAlreadySubscribed(safeId, "HARN0001234 in unexpected place")
    val exception = connector.subscribeToAgentServices(safeId, subscribeAgentRequest).failed.futureValue
    exception shouldBe a[Throwable]
    HipStubs.verifySubscribeToAgentServices()

  "throws an exception when the API response is anything else" in:
    HipStubs.stubSubscribeToAgentServicesFailure(safeId)
    val exception = connector.subscribeToAgentServices(safeId, subscribeAgentRequest).failed.futureValue
    exception shouldBe a[Throwable]
    HipStubs.verifySubscribeToAgentServices()
