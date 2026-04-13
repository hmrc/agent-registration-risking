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

import org.scalatest.RecoverMethods.recoverToExceptionIf
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.agentregistrationrisking.model.RiskingResultRecord
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.*
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.ObjectStoreStubs
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class ResultsFileServiceSpec
extends ISpec:

  val service: ResultsFileService = app.injector.instanceOf[ResultsFileService]

  "downloadAndParseRecords should successfully download and parse the result file" in:
    given RequestHeader = FakeRequest()
    ObjectStoreStubs.stubDownloadMinervaFile(testAvailableFile.downloadURL)

    val result: Future[List[RiskingResultRecord]] = service.downloadAndParseRecords(testAvailableFile)
    val expected: List[RiskingResultRecord] = List(passRecord1, passRecord2)

    result.futureValue shouldBe expected

  "downloadAndParseRecords should handle download failures correctly" in:
    given RequestHeader = FakeRequest()
    ObjectStoreStubs.stubDownloadMinervaFileFailure(testAvailableFile.downloadURL)

    val result: Future[List[RiskingResultRecord]] = service.downloadAndParseRecords(testAvailableFile)
    val exception: UpstreamErrorResponse = recoverToExceptionIf[UpstreamErrorResponse](result).futureValue

    exception.statusCode shouldBe 500
    exception.message should include("Error when downloading file")
    exception.message should include(s"failure when retrieving file at ${testAvailableFile.downloadURL}")
