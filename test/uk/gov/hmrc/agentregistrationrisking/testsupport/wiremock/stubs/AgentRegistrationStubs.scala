/*
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

package uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs

import com.github.tomakehurst.wiremock.client.WireMock as wm
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeRequest
import uk.gov.hmrc.agentregistration.shared.risking.updates.UpdateApplicationStateSentToMinervaRequest

import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.StubMaker

object AgentRegistrationStubs:

  private def sendRiskingOutcomeUrl(applicationReference: ApplicationReference): String =
    s"/agent-registration/risking-updates/risking-outcome/${applicationReference.value}"

  def stubSendRiskingOutcome(
    applicationReference: ApplicationReference,
    riskingOutcomeRequest: RiskingOutcomeRequest
  ): StubMapping = stub(
    applicationReference,
    riskingOutcomeRequest,
    responseStatus = 200
  )

  def stubSendRiskingOutcomeFailure(
    applicationReference: ApplicationReference,
    riskingOutcomeRequest: RiskingOutcomeRequest
  ): StubMapping = stub(
    applicationReference,
    riskingOutcomeRequest,
    responseStatus = 500
  )

  private def stub(
    applicationReference: ApplicationReference,
    riskingOutcomeRequest: RiskingOutcomeRequest,
    responseStatus: Int
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(sendRiskingOutcomeUrl(applicationReference)),
    requestBody = Some(equalToJson(Json.prettyPrint(Json.toJson(riskingOutcomeRequest)))),
    responseStatus = responseStatus
  )

  def verifySendRiskingOutcome(
    applicationReference: ApplicationReference,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(sendRiskingOutcomeUrl(applicationReference)),
    count = count
  )

  def stubUpdateApplicationStateSentToMinerva(applicationReferences: Seq[ApplicationReference]): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlPathEqualTo("/agent-registration/risking-updates/sent-to-minerva"),
    responseStatus = 200,
    requestBody = Some(wm.equalToJson(Json.toJson(UpdateApplicationStateSentToMinervaRequest(applicationReferences)).toString))
  )

  def verifyUpdateApplicationStateSentToMinerva(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlPathEqualTo("/agent-registration/risking-updates/sent-to-minerva"),
    count = count
  )
