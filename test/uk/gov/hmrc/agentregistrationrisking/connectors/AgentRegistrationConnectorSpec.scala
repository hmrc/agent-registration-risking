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

import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeRequest
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.AgentRegistrationStubs

import java.time.LocalDate

class AgentRegistrationConnectorSpec
extends ISpec:

  val connector: AgentRegistrationConnector = app.injector.instanceOf[AgentRegistrationConnector]

  private val applicationReference: ApplicationReference = ApplicationReference("APPREF_TEST")
  private val riskingOutcomeRequest: RiskingOutcomeRequest = RiskingOutcomeRequest(
    riskingCompletedDate = LocalDate.of(2026, 6, 25),
    correctiveActionExpiryDate = None,
    applicationOutcome = RiskingOutcomeApplication.Outcome.Approved,
    entityFailures = Seq.empty,
    individualFailures = Seq.empty
  )

  "completes successfully when the agent-registration service responds with 200 OK" in:
    given RequestHeader = FakeRequest()
    AgentRegistrationStubs.stubSendRiskingOutcome(applicationReference, riskingOutcomeRequest)
    connector.sendRiskingOutcome(applicationReference, riskingOutcomeRequest).futureValue shouldBe (())
    AgentRegistrationStubs.verifySendRiskingOutcome(applicationReference)

  "fails when the agent-registration service responds with a non-2xx status" in:
    given RequestHeader = FakeRequest()
    AgentRegistrationStubs.stubSendRiskingOutcomeFailure(applicationReference, riskingOutcomeRequest)
    val exception = connector.sendRiskingOutcome(applicationReference, riskingOutcomeRequest).failed.futureValue
    exception shouldBe a[Throwable]
    AgentRegistrationStubs.verifySendRiskingOutcome(applicationReference)
