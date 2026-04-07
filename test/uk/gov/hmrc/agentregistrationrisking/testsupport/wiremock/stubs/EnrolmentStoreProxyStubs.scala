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
import uk.gov.hmrc.agentregistration.shared.GroupId
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector.EnrolmentRequest
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.StubMaker

object EnrolmentStoreProxyStubs:

  def stubAllocateEnrolmentToGroup(
                          groupId: GroupId,
                          enrolmentKey: String,
                          enrolmentRequest: EnrolmentRequest
                        ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"/enrolment-store-proxy/enrolment-store/groups/${groupId.value}/enrolments/$enrolmentKey"),
    responseStatus = 201,
    requestBody = Some(equalToJson(Json.toJson(enrolmentRequest).toString()))
  )

  def verifyAllocateEnrolmentToGroup(
                                      groupId: GroupId,
                                      enrolmentKey: String,
                            count: Int = 1
                          ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"/enrolment-store-proxy/enrolment-store/groups/${groupId.value}/enrolments/$enrolmentKey"),
    count = count
  )
