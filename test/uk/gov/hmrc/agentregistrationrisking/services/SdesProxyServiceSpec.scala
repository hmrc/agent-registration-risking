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

package uk.gov.hmrc.agentregistrationrisking.services

import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.SdesProxyStubs

class SdesProxyServiceSpec
extends ISpec:

  val service: SdesProxyService = app.injector.instanceOf[SdesProxyService]

  "notifySdesFileReady sends the expected request to SDES" in:

    given RequestHeader = FakeRequest()

    val objectSummaryWithMd5 = tdAll.objectSummaryWithMd5
    SdesProxyStubs.stubSdesFileReady

    service.notifySdesFileReady(objectSummaryWithMd5).futureValue

    SdesProxyStubs.verifySdesFileReady()

    val requestBody = Json.parse(SdesProxyStubs.getSdesFileReadyRequestBody)

    (requestBody \ "informationType").as[String] shouldBe "2222222"
    (requestBody \ "file" \ "recipientOrSender").as[String] shouldBe "srn"
    (requestBody \ "file" \ "name").as[String] shouldBe objectSummaryWithMd5.location.fileName
    (requestBody \ "file" \ "location").as[String] shouldBe objectSummaryWithMd5.location.asUri
    (requestBody \ "file" \ "checksum" \ "algorithm").as[String] shouldBe "md5"
    (requestBody \ "file" \ "checksum" \ "value").as[String] shouldBe objectSummaryWithMd5.contentMd5.value
    (requestBody \ "file" \ "size").as[Int] shouldBe objectSummaryWithMd5.contentLength.intValue

    val correlationId = (requestBody \ "audit" \ "correlationId").as[String]
    correlationId.nonEmpty shouldBe true

  "notifySdesFileReady throws when SDES returns a non-2xx response" in:

    given RequestHeader = FakeRequest()

    val objectSummaryWithMd5 = tdAll.objectSummaryWithMd5
    SdesProxyStubs.stubSdesFileReadyFailure

    val exception = service.notifySdesFileReady(objectSummaryWithMd5).failed.futureValue

    exception shouldBe a[Throwable]
    SdesProxyStubs.verifySdesFileReady()
