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
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.SafeId
import uk.gov.hmrc.agentregistration.shared.risking.RiskingStatus
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRiskingId
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRiskingId
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileName
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.*
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.EnrolmentStoreProxyStubs
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.HipStubs
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.ObjectStoreStubs
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.SdesProxyStubs

class SdesProxyServiceSpec
extends ISpec:

  val service: SdesProxyService = app.injector.instanceOf[SdesProxyService]
  val repo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  val individualRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  val appRef = ApplicationReference("ABC123456")

  private def setupApplicationWithIndividual() =
    val application = tdAll.llpApplicationForRisking.copy(
      _id = ApplicationForRiskingId("test-app"),
      riskingFileName = Some(RiskingFileName("submitted-file"))
    )
    val individual = tdAll.readyForSubmissionIndividual(application._id).copy(
      _id = IndividualForRiskingId("test-ind")
    )
    repo.upsert(application).futureValue
    individualRepo.upsert(individual).futureValue
    (application, individual)

  "notifySdesFileReady sends the expected request to SDES" in:

    given RequestHeader = FakeRequest()

    SdesProxyStubs.stubSdesFileReady(tdAll.notifySdesFileReadyRequest)

    service.notifySdesFileReady(tdAll.objectSummaryWithMd5).futureValue

    SdesProxyStubs.verifySdesFileReady()

  "notifySdesFileReady throws when SDES returns a non-2xx response" in:

    given RequestHeader = FakeRequest()

    val objectSummaryWithMd5 = tdAll.objectSummaryWithMd5
    SdesProxyStubs.stubSdesFileReadyFailure(tdAll.notifySdesFileReadyRequest)

    val exception = service.notifySdesFileReady(objectSummaryWithMd5).failed.futureValue

    exception shouldBe a[Throwable]
    SdesProxyStubs.verifySdesFileReady()

  "retrieveAndProcessResultsFile retrieves an unprocessed successful results file and processes accordingly" in:

    given RequestHeader = FakeRequest()

    val (application, individual) = setupApplicationWithIndividual()

    application.failures shouldBe None

    SdesProxyStubs.stubFindAvailableFiles(Seq(tdAll.sdesFileData("resultsFile01.txt"), tdAll.sdesFileData("resultsFile02.txt")))
    ObjectStoreStubs.stubDownloadMinervaFile(testAvailableFile.downloadURL)
    ObjectStoreStubs.stubObjectStoreListObjects()
    ObjectStoreStubs.stubObjectStoreUploadFromUrl(uploadedFilePath = "agent-registration-risking/received-results-files/resultsFile02.txt")
    HipStubs.stubSubscribeToAgentServices(SafeId("X0_SAFE_ID_0X"), "AARN0001234")
    EnrolmentStoreProxyStubs.stubAddKnownFacts("HMRC-AS-AGENT~AgentReferenceNumber~AARN0001234")
    EnrolmentStoreProxyStubs.stubAllocateEnrolmentToGroup("group-id-12345", "HMRC-AS-AGENT~AgentReferenceNumber~AARN0001234")

    val result = service.retrieveAndProcessResultsFiles.futureValue
    ObjectStoreStubs.verifyObjectStoreUploadFromUrl()
    result.size shouldBe 1
    result.headOption.value.location.fileName shouldBe "resultsFile02.txt"
    result.headOption.value.location.directory.value shouldBe "agent-registration-risking/received-results-files"

    val updatedApplication = repo.findByApplicationReference(appRef).futureValue.value
    updatedApplication.failures.value.size shouldBe 0
    updatedApplication.isSubscribed shouldBe true

    val updatedIndividual = individualRepo.findByPersonReference(tdAll.personReference).futureValue.value
    updatedIndividual.failures.value.size shouldBe 0

    HipStubs.verifySubscribeToAgentServices()
    EnrolmentStoreProxyStubs.verifyAddKnownFacts()
    EnrolmentStoreProxyStubs.verifyAllocateEnrolmentToGroup()

  "retrieveAndProcessResultsFile updates failures when results contain fixable failures and does not subscribe" in:

    given RequestHeader = FakeRequest()

    setupApplicationWithIndividual()

    SdesProxyStubs.stubFindAvailableFiles(Seq(tdAll.sdesFileData("resultsFile01.txt"), tdAll.sdesFileData("resultsFile02.txt")))
    ObjectStoreStubs.stubDownloadMinervaFile(testAvailableFile.downloadURL, tdAll.failRecordArrayFileMatchingApp)
    ObjectStoreStubs.stubObjectStoreListObjects()
    ObjectStoreStubs.stubObjectStoreUploadFromUrl(uploadedFilePath = "agent-registration-risking/received-results-files/resultsFile02.txt")

    service.retrieveAndProcessResultsFiles.futureValue

    val updatedApplication = repo.findByApplicationReference(appRef).futureValue.value
    updatedApplication.failures.value.size shouldBe 1
    updatedApplication.isSubscribed shouldBe false

    HipStubs.verifySubscribeToAgentServices(count = 0)
    EnrolmentStoreProxyStubs.verifyAddKnownFacts(count = 0)
    EnrolmentStoreProxyStubs.verifyAllocateEnrolmentToGroup(count = 0)

  "retrieveAndProcessResultsFile still uploads file when HIP subscribe fails" in:

    given RequestHeader = FakeRequest()

    setupApplicationWithIndividual()

    SdesProxyStubs.stubFindAvailableFiles(Seq(tdAll.sdesFileData("resultsFile01.txt"), tdAll.sdesFileData("resultsFile02.txt")))
    ObjectStoreStubs.stubDownloadMinervaFile(testAvailableFile.downloadURL)
    ObjectStoreStubs.stubObjectStoreListObjects()
    ObjectStoreStubs.stubObjectStoreUploadFromUrl(uploadedFilePath = "agent-registration-risking/received-results-files/resultsFile02.txt")
    HipStubs.stubSubscribeToAgentServicesFailure(SafeId("X0_SAFE_ID_0X"))

    service.retrieveAndProcessResultsFiles.futureValue

    val updatedApplication = repo.findByApplicationReference(appRef).futureValue.value
    updatedApplication.isSubscribed shouldBe false

    ObjectStoreStubs.verifyObjectStoreUploadFromUrl()
    HipStubs.verifySubscribeToAgentServices()
    EnrolmentStoreProxyStubs.verifyAddKnownFacts(count = 0)
    EnrolmentStoreProxyStubs.verifyAllocateEnrolmentToGroup(count = 0)

  "retrieveAndProcessResultsFile still uploads file when addKnownFacts fails" in:

    given RequestHeader = FakeRequest()

    setupApplicationWithIndividual()

    SdesProxyStubs.stubFindAvailableFiles(Seq(tdAll.sdesFileData("resultsFile01.txt"), tdAll.sdesFileData("resultsFile02.txt")))
    ObjectStoreStubs.stubDownloadMinervaFile(testAvailableFile.downloadURL)
    ObjectStoreStubs.stubObjectStoreListObjects()
    ObjectStoreStubs.stubObjectStoreUploadFromUrl(uploadedFilePath = "agent-registration-risking/received-results-files/resultsFile02.txt")
    HipStubs.stubSubscribeToAgentServices(SafeId("X0_SAFE_ID_0X"), "AARN0001234")
    EnrolmentStoreProxyStubs.stubAddKnownFactsFailure("HMRC-AS-AGENT~AgentReferenceNumber~AARN0001234")

    service.retrieveAndProcessResultsFiles.futureValue

    val updatedApplication = repo.findByApplicationReference(appRef).futureValue.value
    updatedApplication.isSubscribed shouldBe false
    ObjectStoreStubs.verifyObjectStoreUploadFromUrl()

  "retrieveAndProcessResultsFile still uploads file when allocateEnrolmentToGroup fails" in:

    given RequestHeader = FakeRequest()

    setupApplicationWithIndividual()

    SdesProxyStubs.stubFindAvailableFiles(Seq(tdAll.sdesFileData("resultsFile01.txt"), tdAll.sdesFileData("resultsFile02.txt")))
    ObjectStoreStubs.stubDownloadMinervaFile(testAvailableFile.downloadURL)
    ObjectStoreStubs.stubObjectStoreListObjects()
    ObjectStoreStubs.stubObjectStoreUploadFromUrl(uploadedFilePath = "agent-registration-risking/received-results-files/resultsFile02.txt")
    HipStubs.stubSubscribeToAgentServices(SafeId("X0_SAFE_ID_0X"), "AARN0001234")
    EnrolmentStoreProxyStubs.stubAddKnownFacts("HMRC-AS-AGENT~AgentReferenceNumber~AARN0001234")
    EnrolmentStoreProxyStubs.stubAllocateEnrolmentToGroupFailure("group-id-12345", "HMRC-AS-AGENT~AgentReferenceNumber~AARN0001234")

    service.retrieveAndProcessResultsFiles.futureValue

    val updatedApplication = repo.findByApplicationReference(appRef).futureValue.value
    updatedApplication.isSubscribed shouldBe false
    ObjectStoreStubs.verifyObjectStoreUploadFromUrl()

  "retrieveAndProcessResultsFile does not upload to object store if the file was not processed successfully" in:
    given RequestHeader = FakeRequest()

    SdesProxyStubs.stubFindAvailableFilesFailure
    val result = service.retrieveAndProcessResultsFiles
    ObjectStoreStubs.verifyObjectStoreUploadFromUrl(count = 0)

  "retrieveAndProcessResultsFile still subscribes valid application when results contain a non-existent application reference" in:

    given RequestHeader = FakeRequest()

    setupApplicationWithIndividual()

    SdesProxyStubs.stubFindAvailableFiles(Seq(tdAll.sdesFileData("resultsFile01.txt"), tdAll.sdesFileData("resultsFile02.txt")))
    ObjectStoreStubs.stubDownloadMinervaFile(testAvailableFile.downloadURL, tdAll.passRecordArrayFileWithNonExistentApp)
    ObjectStoreStubs.stubObjectStoreListObjects()
    ObjectStoreStubs.stubObjectStoreUploadFromUrl(uploadedFilePath = "agent-registration-risking/received-results-files/resultsFile02.txt")
    HipStubs.stubSubscribeToAgentServices(SafeId("X0_SAFE_ID_0X"), "AARN0001234")
    EnrolmentStoreProxyStubs.stubAddKnownFacts("HMRC-AS-AGENT~AgentReferenceNumber~AARN0001234")
    EnrolmentStoreProxyStubs.stubAllocateEnrolmentToGroup("group-id-12345", "HMRC-AS-AGENT~AgentReferenceNumber~AARN0001234")

    service.retrieveAndProcessResultsFiles.futureValue

    val updatedApplication = repo.findByApplicationReference(appRef).futureValue.value
    updatedApplication.isSubscribed shouldBe true
    ObjectStoreStubs.verifyObjectStoreUploadFromUrl()
    HipStubs.verifySubscribeToAgentServices()
