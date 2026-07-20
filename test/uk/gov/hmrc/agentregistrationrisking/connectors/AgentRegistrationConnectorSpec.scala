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
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcome
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeRequest
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.AgentRegistrationStubs

import java.time.Instant
import java.time.LocalDate

class AgentRegistrationConnectorSpec
extends ISpec:

  val agentRegistrationConnector: AgentRegistrationConnector = app.injector.instanceOf[AgentRegistrationConnector]

  private val applicationReference: ApplicationReference = ApplicationReference("APPREF_TEST")
  private val riskingOutcomeRequest: RiskingOutcomeRequest = RiskingOutcomeRequest(
    emailsSentAt = Instant.parse("2026-06-25T11:33:55Z"),
    applicationOutcome = RiskingOutcome.Approved,
    entityFailures = Seq.empty,
    entityOutcome = RiskingOutcome.Approved,
    individualFailures = Seq.empty
  )

  "completes successfully when the agent-registration service responds with 200 OK" in:
    given RequestHeader = FakeRequest()
    AgentRegistrationStubs.stubSendRiskingOutcome(
      applicationReference,
      riskingOutcomeRequest,
      expectedAuthorizationToken = tdAll.internalAuthToken
    )
    agentRegistrationConnector.sendRiskingOutcome(applicationReference, riskingOutcomeRequest).futureValue shouldBe (())
    AgentRegistrationStubs.verifySendRiskingOutcome(applicationReference, expectedAuthorizationToken = tdAll.internalAuthToken)

  "fails when the agent-registration service responds with a non-2xx status" in:
    given RequestHeader = FakeRequest()
    AgentRegistrationStubs.stubSendRiskingOutcomeFailure(
      applicationReference,
      riskingOutcomeRequest,
      expectedAuthorizationToken = tdAll.internalAuthToken
    )
    val exception = agentRegistrationConnector.sendRiskingOutcome(applicationReference, riskingOutcomeRequest).failed.futureValue
    exception shouldBe a[Throwable]
    AgentRegistrationStubs.verifySendRiskingOutcome(applicationReference, expectedAuthorizationToken = tdAll.internalAuthToken)
