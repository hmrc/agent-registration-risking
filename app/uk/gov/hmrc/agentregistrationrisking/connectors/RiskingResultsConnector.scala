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
import uk.gov.hmrc.agentregistrationrisking.model.RiskingResultRaw
import uk.gov.hmrc.agentregistrationrisking.model.sdes.AvailableFile
import uk.gov.hmrc.http.client.HttpClientV2

import java.net.URI
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class RiskingResultsConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2
)(using ExecutionContext)
extends Connector:

  def getRiskingFile(availableFile: AvailableFile)(using RequestHeader): Future[List[RiskingResultRaw]] =
    val fileLocation: URL = new URI(availableFile.downloadURL).toURL
    httpClient
      .get(fileLocation)
      .withProxy
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case OK => response.json.as[List[RiskingResultRaw]]
          case _ =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = fileLocation,
              status = response.status,
              response = response,
              info = s"failure when retrieving file at ${availableFile.downloadURL}"
            )
