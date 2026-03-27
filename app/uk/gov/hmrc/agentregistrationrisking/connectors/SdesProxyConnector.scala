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

  private val headers: Seq[(String, String)] = Seq(
    "x-client-id" -> appConfig.sdesServerToken.value,
    "X-SDES-Key" -> appConfig.sdesSrn.value
  )
  private val availableFilesUrl: URL = url"${appConfig.sdesProxyBaseUrl}/files-available/list/${appConfig.sdesInformationType.value}"

  def listAvailableFiles(using RequestHeader): Future[Seq[AvailableFile]] = httpClient
    .get(availableFilesUrl)
    .setHeader(headers*)
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

  private val notifySdesFileReadyUrl: URL = url"${appConfig.sdesProxyBaseUrl}/notification/fileready"

  def notifySdesFileReady(notifySdesFileReadyRequest: NotifySdesFileReadyRequest)(using
    RequestHeader
  ): Future[Unit] = httpClient
    .post(notifySdesFileReadyUrl)
    .setHeader(headers*)
    .withBody(Json.toJson(notifySdesFileReadyRequest))
    .execute[HttpResponse]
    .map: response =>
      response.status match
        // Do we want to return something here, if so what?
        case status if is2xx(status) => ()
        case status =>
          Errors.throwUpstreamErrorResponse(
            httpMethod = "POST",
            url = notifySdesFileReadyUrl,
            status = status,
            response = response
          )
    // Is this logging enough?
    .andLogOnFailure(s"Failed to send notification")
