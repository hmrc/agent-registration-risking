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
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.StubMaker
import uk.gov.hmrc.agentregistrationrisking.testsupport.RichMatchers.*

object ObjectStoreStubs:

  private def urlPattern(
    fileName: String,
    owner: String = "agent-registration-risking",
    directory: String = "applications-for-risking"
  ): String = s"/object-store/object/$owner/$directory/$fileName"

  def stubObjectStoreTransfer(
    fileName: String,
    owner: String = "agent-registration-risking",
    directory: String = "applications-for-risking",
    response: JsObject = TdAll.tdAll.objectStoreUploadResponse
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.PUT,
    urlPattern = wm.urlEqualTo(urlPattern(
      fileName,
      owner,
      directory
    )),
    responseStatus = 200,
    responseBody = Json.prettyPrint(response)
  )

  def verifyObjectStoreTransfer(
    fileName: String,
    owner: String = "agent-registration-risking",
    directory: String = "applications-for-risking",
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.PUT,
    urlPattern = wm.urlEqualTo(urlPattern(
      fileName,
      owner,
      directory
    )),
    count = count
  )

  def getRequestBody(
    fileName: String,
    owner: String = "agent-registration-risking",
    directory: String = "applications-for-risking"
  ): String =

    StubMaker.getEvents((x: ServeEvent) =>
      x.getRequest.getUrl === urlPattern(
        fileName,
        owner,
        directory
      )
    )
      .lastOption
      .value
      .getRequest
      .getBodyAsString

  def stubObjectStoreUploadFromUrl(
    uploadedFilePath: String
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"/object-store/ops/upload-from-url"),
    responseStatus = 200,
    responseBody = Json.prettyPrint(TdAll.tdAll.objectStoreUploadFromUrlResponse(uploadedFilePath))
  )

  def stubObjectStoreUploadFromUrlFailure: StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"/object-store/ops/upload-from-url"),
    responseStatus = 500,
    responseBody = Json.prettyPrint(Json.obj("error" -> "Some Error"))
  )

  def verifyObjectStoreUploadFromUrl(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"/object-store/ops/upload-from-url"),
    count = count
  )
