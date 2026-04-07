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
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.SafeId
import uk.gov.hmrc.agentregistrationrisking.model.hip.Arn
import uk.gov.hmrc.agentregistrationrisking.model.hip.SubscribeAgentRequest
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.StubMaker

object HipStubs:

  def stubSubscribeAgent(
    safeId: SafeId,
    subscribeAgentRequest: SubscribeAgentRequest,
    arn: Arn
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"/etmp/RESTAdapter/generic/agent/subscription/${safeId.value}"),
    responseStatus = 200,
    responseBody = Json.toJson(arn).toString(),
    requestBody = Some(equalToJson(Json.toJson(subscribeAgentRequest).toString()))
  )

  def verifySubscribeAgent(
                            safeId: SafeId,
                            count: Int = 1
                          ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"/etmp/RESTAdapter/generic/agent/subscription/${safeId.value}"),
    count = count
  )
