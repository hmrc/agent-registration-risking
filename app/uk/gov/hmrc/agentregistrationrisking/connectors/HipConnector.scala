/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.http.Status.*
import play.api.libs.json.*
import uk.gov.hmrc.agentregistration.shared.SafeId
import uk.gov.hmrc.agentregistration.shared.util.Errors
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.model.CorrelationIdGenerator
import uk.gov.hmrc.agentregistrationrisking.model.hip.Arn
import uk.gov.hmrc.agentregistrationrisking.model.hip.HipAuthToken
import uk.gov.hmrc.agentregistrationrisking.model.hip.SubscribeAgentRequest
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2

import java.time.Instant
import java.time.temporal.ChronoUnit.SECONDS
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class HipConnector @Inject() (
  appConfig: AppConfig,
  correlationIdGenerator: CorrelationIdGenerator,
  http: HttpClientV2
)(using ExecutionContext)
extends Connector:

  private val baseUrl: String = appConfig.hipBaseUrl
  private val hipAuthToken: HipAuthToken = appConfig.hipAuthToken
  private val originatingSystem: String = "MDTP-ASA"
  private val transmittingSystem: String = "HIP"

  private val hipHeaders: Seq[(String, String)] = Seq(
    "Authorization" -> s"Basic ${hipAuthToken.value}",
    "correlationid" -> correlationIdGenerator.nextCorrelationId.value,
    "X-Originating-System" -> originatingSystem,
    "X-Receipt-Date" -> Instant.now().truncatedTo(SECONDS).toString,
    "X-Transmitting-System" -> transmittingSystem
  )

  /** Subscribes an agent to Agent Services in HIP, which creates and returns the agent's ARN.
    *
    * API specification:
    * https://admin.tax.service.gov.uk/integration-hub/apis/view-specification/ed3bdeb8-6db7-4c20-91c9-8b144aa1736b/test#tag/Agent-Subscription
    */
  def subscribeToAgentServices(
    safeId: SafeId,
    subscribeAgentRequest: SubscribeAgentRequest
  )(implicit
    rh: RequestHeader
  ): Future[Arn] = {
    val url = s"$baseUrl/etmp/RESTAdapter/generic/agent/subscription/${safeId.value}"
    http
      .post(url"$url")
      .setHeader(hipHeaders*)
      .withBody(Json.toJson(subscribeAgentRequest))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case CREATED => (response.json \ "success" \ "arn").as[Arn]
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "POST",
              url = url"$url",
              status = status,
              response = response,
              info = "subscribeToAgentServices problem"
            )
        }
      }
  }
