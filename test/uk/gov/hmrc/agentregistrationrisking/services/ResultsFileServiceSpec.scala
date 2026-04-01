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
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.risking.IndividualRiskingOutcome
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.SdesProxyStubs
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.ObjectStoreStubs
import uk.gov.hmrc.agentregistration.shared.risking.IndividualRiskingOutcome.*

class ResultsFileServiceSpec
extends ISpec:

  val service: ResultsFileService = app.injector.instanceOf[ResultsFileService]
  val repo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]

  "retrieveAndProcessResultsFile retrieves all unprocessed results files and processes accordingly" in:

    given RequestHeader = FakeRequest()

    repo.upsert(
      (tdAll.llpApplicationForRisking.copy(applicationReference = ApplicationReference("1234")))
    ).futureValue shouldBe () withClue "ensure there is an application for risking in mongo before http request"

    val existingApplication =
      repo.findByApplicationReference(
        ApplicationReference("1234")
      ).futureValue.value
    val existingIndividual = existingApplication.individuals.headOption.value

    existingApplication.failures shouldBe None
    existingIndividual.failures shouldBe None

    SdesProxyStubs.stubFindAvailableFiles(Seq(tdAll.sdesFileData("resultsFile01.txt"), tdAll.sdesFileData("resultsFile02.txt")))
    ObjectStoreStubs.stubObjectStoreListObjects()
    ObjectStoreStubs.stubObjectStoreUploadFromUrl(uploadedFilePath = "agent-registration-risking/received-results-files/resultsFile02.txt")

    val result = service.retrieveAndProcessResultsFiles.futureValue
    ObjectStoreStubs.verifyObjectStoreUploadFromUrl()
    result.size shouldBe 1
    result.headOption.value.location.fileName shouldBe "resultsFile02.txt"
    result.headOption.value.location.directory.value shouldBe "agent-registration-risking/received-results-files"

    val updatedApplication =
      repo.findByApplicationReference(
        ApplicationReference("1234")
      ).futureValue.value
    val updatedIndividual = updatedApplication.individuals.headOption.value

    updatedApplication.failures.value.size shouldBe 1
    updatedIndividual.failures.value.size shouldBe 1
    updatedIndividual.failures.value.outcome shouldBe IndividualRiskingOutcome.FailedFixable

  "retrieveAndProcessResultsFile does not upload to object store if the file was not processed successfully" in:

    given RequestHeader = FakeRequest()

    SdesProxyStubs.stubFindAvailableFilesFailure
    val result = service.retrieveAndProcessResultsFiles
    ObjectStoreStubs.verifyObjectStoreUploadFromUrl(count = 0)
