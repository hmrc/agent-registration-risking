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
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.StubMaker

object AuditStubs:
  
  private val auditUrl: String = "/write/audit"
  private val auditMergedUrl: String = "/write/audit/merged"

  def stubAuditWrite(): Unit =
    List(auditUrl, auditMergedUrl).foreach: url =>
      StubMaker.make(
        httpMethod = StubMaker.HttpMethod.POST,
        urlPattern = wm.urlEqualTo(url),
        responseStatus = 204
      )

  def verifyAuditSent(
    auditType: String,
    detail: JsValue,
    count: Int = 1
  ): Unit =
    wm.verify(
      count,
      wm.postRequestedFor(wm.urlEqualTo(auditUrl))
        .withRequestBody(wm.equalToJson(
          Json.stringify(Json.obj(
            "auditType" -> auditType,
            "detail" -> detail
          )),
          true, // ignoreArrayOrder
          true // ignoreExtraElements
        ))
    )
  
  def verifyNoAuditSent(): Unit = wm.verify(0, wm.postRequestedFor(wm.urlEqualTo(auditUrl)))
