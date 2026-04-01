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

package uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs

import com.github.tomakehurst.wiremock.client.WireMock as wm
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.OptionValues.convertOptionToValuable
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistrationrisking.model.sdes.AvailableFile
import uk.gov.hmrc.agentregistrationrisking.model.sdes.NotifySdesFileReadyRequest
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.StubMaker

object SdesProxyStubs:

  def stubFindAvailableFiles(
    response: Seq[AvailableFile]
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlEqualTo(s"/files-available/list/test-inbound-information-type"),
    responseStatus = 200,
    responseBody = Json.toJson(response).toString()
  )

  def stubFindAvailableFilesFailure: StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlEqualTo(s"/files-available/list/test-inbound-information-type"),
    responseStatus = 500,
    responseBody = Json.prettyPrint(Json.obj("error" -> "Some Error"))
  )

  def stubSdesFileReady(body: NotifySdesFileReadyRequest): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"/notification/fileready"),
    responseStatus = 200,
    requestHeaders = Seq(
      "x-client-id" -> wm.equalTo("test-outbound-server-token")
    ),
    requestBody = Some(equalToJson(Json.prettyPrint(Json.toJson(body))))
  )

  def stubSdesFileReadyFailure(body: NotifySdesFileReadyRequest): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"/notification/fileready"),
    responseStatus = 500,
    requestHeaders = Seq(
      "x-client-id" -> wm.equalTo("test-outbound-server-token")
    ),
    requestBody = Some(equalToJson(Json.prettyPrint(Json.toJson(body))))
  )

  def verifySdesFileReady(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"/notification/fileready"),
    count = count
  )

  def getSdesFileReadyRequestBody: String =
    StubMaker.getEvents((x: ServeEvent) =>
      x.getRequest.getUrl == "/notification/fileready"
    )
      .lastOption
      .value
      .getRequest
      .getBodyAsString
