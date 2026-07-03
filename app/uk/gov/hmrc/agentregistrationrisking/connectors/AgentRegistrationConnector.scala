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

import play.api.http.Status.OK
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeRequest
import uk.gov.hmrc.agentregistration.shared.util.Errors
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.util.FutureUtil.andLogOnFailure
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class AgentRegistrationConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2
)(using ExecutionContext)
extends Connector:

  def sendRiskingOutcome(
    applicationReference: ApplicationReference,
    riskingOutcomeRequest: RiskingOutcomeRequest
  )(using RequestHeader): Future[Unit] =
    val url: URL = url"$baseUrl/agent-registration/risking-updates/risking-outcome/${applicationReference.value}"
    httpClient
      .post(url)
      .withBody(Json.toJson(riskingOutcomeRequest))
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case OK => ()
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "POST",
              url = url,
              status = status,
              response = response,
              info = s"Failed to send risking outcome for applicationReference [${applicationReference.value}]"
            )
      .andLogOnFailure(s"Failed to send risking outcome for applicationReference [${applicationReference.value}]")

  private val baseUrl: String = appConfig.agentRegistrationBaseUrl
