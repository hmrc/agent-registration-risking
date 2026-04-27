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

package uk.gov.hmrc.agentregistrationrisking.controllers

import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import play.api.mvc.Call
import uk.gov.hmrc.agentregistration.shared.SafeId
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRiskingId
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRiskingId
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileId
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.*
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.EnrolmentStoreProxyStubs
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.HipStubs
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.ObjectStoreStubs
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.SdesProxyStubs

class SdesNotificationControllerSpec
extends ControllerSpec:

  val path: String = s"/agent-registration-risking/receive-sdes-notifications"
  val repo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  val individualRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  val appRef = ApplicationReference("ABC123456")

  private def setupApplicationWithIndividual() =
    val application = tdAll.llpApplicationForRisking.copy(
      _id = ApplicationForRiskingId("test-app"),
      riskingFileId = Some(RiskingFileId("submitted-file"))
    )
    val individual = tdAll.readyForSubmissionIndividual(application._id).copy(
      _id = IndividualForRiskingId("test-ind")
    )
    repo.upsert(application).futureValue
    individualRepo.upsert(individual).futureValue

  "SDES Notification controller should have the correct route" in:
    val call: Call = routes.SdesNotificationController.receiveSdesNotification
    call shouldBe Call(
      method = "POST",
      url = path
    )

  "receiveNotification should handle the File Ready Notification with no matching application in DB" in:
    val SDESNotification: JsObject = fileReadyNotification

    SdesProxyStubs.stubFindAvailableFiles(Seq(tdAll.sdesFileData("resultsFile01.txt"), tdAll.sdesFileData("resultsFile02.txt")))
    ObjectStoreStubs.stubDownloadMinervaFile(testAvailableFile.downloadURL)
    ObjectStoreStubs.stubObjectStoreListObjects()
    ObjectStoreStubs.stubObjectStoreUploadFromUrl(uploadedFilePath = "agent-registration-risking/received-results-files/resultsFile02.txt")

    val response: WSResponse =
      wsClient
        .url(s"$baseUrl/agent-registration-risking/receive-sdes-notifications")
        .post(SDESNotification)
        .futureValue

    response.status shouldBe Status.OK
    ObjectStoreStubs.verifyObjectStoreUploadFromUrl()
    HipStubs.verifySubscribeToAgentServices(count = 0)
    EnrolmentStoreProxyStubs.verifyAddKnownFacts(count = 0)
    EnrolmentStoreProxyStubs.verifyAllocateEnrolmentToGroup(count = 0)

  "receiveNotification should process approved application end-to-end: file processing, status update, subscribe, and enrol" in:
    setupApplicationWithIndividual()

    SdesProxyStubs.stubFindAvailableFiles(Seq(tdAll.sdesFileData("resultsFile01.txt"), tdAll.sdesFileData("resultsFile02.txt")))
    ObjectStoreStubs.stubDownloadMinervaFile(testAvailableFile.downloadURL)
    ObjectStoreStubs.stubObjectStoreListObjects()
    ObjectStoreStubs.stubObjectStoreUploadFromUrl(uploadedFilePath = "agent-registration-risking/received-results-files/resultsFile02.txt")
    HipStubs.stubSubscribeToAgentServices(SafeId("X0_SAFE_ID_0X"), "AARN0001234")
    EnrolmentStoreProxyStubs.stubAddKnownFacts("HMRC-AS-AGENT~AgentReferenceNumber~AARN0001234")
    EnrolmentStoreProxyStubs.stubAllocateEnrolmentToGroup("group-id-12345", "HMRC-AS-AGENT~AgentReferenceNumber~AARN0001234")

    val response: WSResponse =
      wsClient
        .url(s"$baseUrl/agent-registration-risking/receive-sdes-notifications")
        .post(fileReadyNotification)
        .futureValue

    response.status shouldBe Status.OK

    val afterProcessing = repo.findByApplicationReference(appRef).futureValue.value
    afterProcessing.failures.value.size shouldBe 0
    afterProcessing.isSubscribed shouldBe true

    val updatedIndividual = individualRepo.findByPersonReference(tdAll.personReference).futureValue.value
    updatedIndividual.failures.value.size shouldBe 0

    ObjectStoreStubs.verifyObjectStoreUploadFromUrl()
    HipStubs.verifySubscribeToAgentServices()
    EnrolmentStoreProxyStubs.verifyAddKnownFacts()
    EnrolmentStoreProxyStubs.verifyAllocateEnrolmentToGroup()

  "receiveNotification should process failed application end-to-end: file processing, status update, no subscribe" in:
    setupApplicationWithIndividual()

    SdesProxyStubs.stubFindAvailableFiles(Seq(tdAll.sdesFileData("resultsFile01.txt"), tdAll.sdesFileData("resultsFile02.txt")))
    ObjectStoreStubs.stubDownloadMinervaFile(testAvailableFile.downloadURL, tdAll.failRecordArrayFileMatchingApp)
    ObjectStoreStubs.stubObjectStoreListObjects()
    ObjectStoreStubs.stubObjectStoreUploadFromUrl(uploadedFilePath = "agent-registration-risking/received-results-files/resultsFile02.txt")

    val response: WSResponse =
      wsClient
        .url(s"$baseUrl/agent-registration-risking/receive-sdes-notifications")
        .post(fileReadyNotification)
        .futureValue

    response.status shouldBe Status.OK

    val afterProcessing = repo.findByApplicationReference(appRef).futureValue.value
    afterProcessing.failures.value.size shouldBe 1
    afterProcessing.isSubscribed shouldBe false

    ObjectStoreStubs.verifyObjectStoreUploadFromUrl()
    HipStubs.verifySubscribeToAgentServices(count = 0)
    EnrolmentStoreProxyStubs.verifyAddKnownFacts(count = 0)
    EnrolmentStoreProxyStubs.verifyAllocateEnrolmentToGroup(count = 0)

  "receiveNotification should return OK even when HIP subscribe fails" in:
    setupApplicationWithIndividual()

    SdesProxyStubs.stubFindAvailableFiles(Seq(tdAll.sdesFileData("resultsFile01.txt"), tdAll.sdesFileData("resultsFile02.txt")))
    ObjectStoreStubs.stubDownloadMinervaFile(testAvailableFile.downloadURL)
    ObjectStoreStubs.stubObjectStoreListObjects()
    ObjectStoreStubs.stubObjectStoreUploadFromUrl(uploadedFilePath = "agent-registration-risking/received-results-files/resultsFile02.txt")
    HipStubs.stubSubscribeToAgentServicesFailure(SafeId("X0_SAFE_ID_0X"))

    val response: WSResponse =
      wsClient
        .url(s"$baseUrl/agent-registration-risking/receive-sdes-notifications")
        .post(fileReadyNotification)
        .futureValue

    response.status shouldBe Status.OK

    val afterProcessing = repo.findByApplicationReference(appRef).futureValue.value
    afterProcessing.isSubscribed shouldBe false

    ObjectStoreStubs.verifyObjectStoreUploadFromUrl()
    HipStubs.verifySubscribeToAgentServices()
    EnrolmentStoreProxyStubs.verifyAddKnownFacts(count = 0)
    EnrolmentStoreProxyStubs.verifyAllocateEnrolmentToGroup(count = 0)

  "receiveNotifications should handle the File Received Notification correctly" in:
    val SDESNotification = fileReceivedNotification

    val response: WSResponse =
      wsClient
        .url(s"$baseUrl/agent-registration-risking/receive-sdes-notifications")
        .post(SDESNotification)
        .futureValue

    response.status shouldBe Status.OK
    response.body[String] === ""

  "receiveNotifications should handle the File Processing Failed Notification correctly" in:
    val SDESNotification = fileProcessingFailureNotification

    val response: WSResponse =
      wsClient
        .url(s"$baseUrl/agent-registration-risking/receive-sdes-notifications")
        .post(SDESNotification)
        .futureValue
    response.status shouldBe Status.OK
    response.body[String] === ""

  "receiveNotification should handle the File Processed Notification correctly" in:
    val SDESNotification = fileProcessedNotification

    val response: WSResponse =
      wsClient
        .url(s"$baseUrl/agent-registration-risking/receive-sdes-notifications")
        .post(SDESNotification)
        .futureValue

    response.status shouldBe Status.OK
    response.body[String] === ""

  "should handle malformed JSON correctly" in:
    val SDESNotification = JsString("{Test Bad Json")

    val response: WSResponse =
      wsClient
        .url(s"$baseUrl/agent-registration-risking/receive-sdes-notifications")
        .post(SDESNotification)
        .futureValue

    response.status shouldBe Status.BAD_REQUEST
    response.body[String] === ""
