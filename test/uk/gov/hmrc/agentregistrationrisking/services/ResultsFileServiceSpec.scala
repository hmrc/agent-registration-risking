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

import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.agentregistrationrisking.repository.ResultsFileLogRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.SDESProxyStubs
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.ObjectStoreStubs

class ResultsFileServiceSpec
extends ISpec:

  val repo: ResultsFileLogRepo = app.injector.instanceOf[ResultsFileLogRepo]
  val service: ResultsFileService = app.injector.instanceOf[ResultsFileService]

  "retrieveAndProcessResultsFile retrieves all unprocessed results files and processes accordingly" in:

    given RequestHeader = FakeRequest()

    val alreadyProcessedFilesUpsert: Unit = repo.upsert(tdAll.resultsFileLog("resultsFile01.txt")).futureValue
    val verifyUpsert = repo.findAll().futureValue.size shouldBe 1

    SDESProxyStubs.stubFindAvailableFiles(Seq(tdAll.sdesFileData("resultsFile01.txt"), tdAll.sdesFileData("resultsFile02.txt")))
    ObjectStoreStubs.stubObjectStoreUploadFromUrl(uploadedFilePath = "agent-registration-risking/received-results-files/resultsFile02.txt")

    val result = service.retrieveAndProcessResultsFiles.futureValue
    ObjectStoreStubs.verifyObjectStoreUploadFromUrl()
    result.size shouldBe 1
    result.headOption.value.location.fileName shouldBe "resultsFile02.txt"
    result.headOption.value.location.directory.value shouldBe "agent-registration-risking/received-results-files"
    val verifyNewUpsert = repo.findAll().futureValue.size shouldBe 2

  "retrieveAndProcessResultsFile doesn't update mongo if file not successfully uploaded to object store" in:

    given RequestHeader = FakeRequest()

    val alreadyProcessedFilesUpsert: Unit = repo.upsert(tdAll.resultsFileLog("resultsFile01.txt")).futureValue
    val verifyUpsert = repo.findAll().futureValue.size shouldBe 1

    SDESProxyStubs.stubFindAvailableFiles(Seq(tdAll.sdesFileData("resultsFile01.txt"), tdAll.sdesFileData("resultsFile02.txt")))
    ObjectStoreStubs.stubObjectStoreUploadFromUrlFailure
    val result = service.retrieveAndProcessResultsFiles
    val verifyNoUpsert = repo.findAll().futureValue.size shouldBe 1
