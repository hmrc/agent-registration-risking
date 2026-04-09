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
import uk.gov.hmrc.agentregistration.shared.util.Errors
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.model.sdes.AvailableFile
import uk.gov.hmrc.agentregistrationrisking.model.sdes.NotifySdesFileReadyRequest
import uk.gov.hmrc.agentregistrationrisking.util.FutureUtil.andLogOnFailure
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SdesProxyConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2
)(using ExecutionContext)
extends Connector:

  private val listAvailableFilesHeaders: Seq[(String, String)] = Seq(
    "X-Client-ID" -> appConfig.SdesProxy.inboundServerToken.value,
    "X-SDES-Key" -> appConfig.SdesProxy.srn.value
  )
  private val availableFilesUrl: URL = url"${appConfig.SdesProxy.baseUrl}/files-available/list/${appConfig.SdesProxy.inboundInformationType.value}"

  def listAvailableFiles(using RequestHeader): Future[Seq[AvailableFile]] = httpClient
    .get(availableFilesUrl)
    .setHeader(listAvailableFilesHeaders*)
    .execute[HttpResponse]
    .map: response =>
      response.status match
        case OK => response.json.as[Seq[AvailableFile]]
        case _ =>
          Errors.throwUpstreamErrorResponse(
            httpMethod = "GET",
            url = availableFilesUrl,
            status = response.status,
            response = response,
            info = "getAvailableResultsFiles problem"
          )

  private val notifySdesFileReadyHeaders: Seq[(String, String)] = Seq(
    "X-Client-ID" -> appConfig.SdesProxy.outboundServerToken.value
  )

  private val notifySdesFileReadyUrl: URL = url"${appConfig.SdesProxy.baseUrl}/notification/fileready"

  def notifySdesFileReady(notifySdesFileReadyRequest: NotifySdesFileReadyRequest)(using
    RequestHeader
  ): Future[Unit] = httpClient
    .post(notifySdesFileReadyUrl)
    .setHeader(notifySdesFileReadyHeaders*)
    .withBody(Json.toJson(notifySdesFileReadyRequest))
    .execute[HttpResponse]
    .map: response =>
      response.status match
        case status if is2xx(status) =>
          logger.info(s"Successfully sent notification to SDES, correlationId: ${notifySdesFileReadyRequest.audit.correlationID}")
          ()
        case status =>
          Errors.throwUpstreamErrorResponse(
            httpMethod = "POST",
            url = notifySdesFileReadyUrl,
            status = status,
            response = response
          )
    .andLogOnFailure(s"Failed to send SDES notification, correlationId: ${notifySdesFileReadyRequest.audit.correlationID}")
