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

package uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs

import com.github.tomakehurst.wiremock.client.WireMock as wm
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.StubMaker

object EnrolmentStoreProxyStubs:

  private val basePath = "/enrolment-store-proxy/enrolment-store"

  def stubAddKnownFacts(enrolmentKey: String): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.PUT,
    urlPattern = wm.urlEqualTo(s"$basePath/enrolments/$enrolmentKey"),
    responseStatus = 204
  )

  def stubAddKnownFactsFailure(enrolmentKey: String): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.PUT,
    urlPattern = wm.urlEqualTo(s"$basePath/enrolments/$enrolmentKey"),
    responseStatus = 500,
    responseBody = Json.stringify(Json.obj("error" -> "Internal Server Error"))
  )

  def stubAllocateEnrolmentToGroup(
    groupId: String,
    enrolmentKey: String
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"$basePath/groups/$groupId/enrolments/$enrolmentKey"),
    responseStatus = 201
  )

  def stubAllocateEnrolmentToGroupFailure(
    groupId: String,
    enrolmentKey: String
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"$basePath/groups/$groupId/enrolments/$enrolmentKey"),
    responseStatus = 500,
    responseBody = Json.stringify(Json.obj("error" -> "Internal Server Error"))
  )

  def verifyAddKnownFacts(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.PUT,
    urlPattern = wm.urlMatching(s"$basePath/enrolments/.*"),
    count = count
  )

  def verifyAllocateEnrolmentToGroup(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlMatching(s"$basePath/groups/.*/enrolments/.*"),
    count = count
  )
