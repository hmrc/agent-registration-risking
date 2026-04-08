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
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationForRiskingStatus
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistration.shared.risking.IndividualRiskingOutcome
import uk.gov.hmrc.agentregistration.shared.risking.PersonReference
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.model.RiskingResultRecord
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.*
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.ObjectStoreStubs
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.SdesProxyStubs
import uk.gov.hmrc.agentregistration.shared.risking.IndividualRiskingOutcome.*
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.SdesProxyStubs
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class SdesProxyServiceSpec
extends ISpec:

  val service: SdesProxyService = app.injector.instanceOf[SdesProxyService]
  val repo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]

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

    repo.upsert(
      (tdAll.llpApplicationForRisking.copy(applicationReference = ApplicationReference("ABC123456")))
    ).futureValue shouldBe () withClue "ensure there is an application for risking in mongo before http request"

    val existingApplication =
      repo.findByApplicationReference(
        ApplicationReference("ABC123456")
      ).futureValue.value
    val existingIndividual = existingApplication.individuals.headOption.value

    existingApplication.failures shouldBe None
    existingIndividual.failures shouldBe None

    SdesProxyStubs.stubFindAvailableFiles(Seq(tdAll.sdesFileData("resultsFile01.txt"), tdAll.sdesFileData("resultsFile02.txt")))
    ObjectStoreStubs.stubDownloadMinervaFile(testAvailableFile.downloadURL)
    ObjectStoreStubs.stubObjectStoreListObjects()
    ObjectStoreStubs.stubObjectStoreUploadFromUrl(uploadedFilePath = "agent-registration-risking/received-results-files/resultsFile02.txt")

    val result = service.retrieveAndProcessResultsFiles.futureValue
    ObjectStoreStubs.verifyObjectStoreUploadFromUrl()
    result.size shouldBe 1
    result.headOption.value.location.fileName shouldBe "resultsFile02.txt"
    result.headOption.value.location.directory.value shouldBe "agent-registration-risking/received-results-files"

    val updatedApplication =
      repo.findByApplicationReference(
        ApplicationReference("ABC123456")
      ).futureValue.value
    val updatedIndividual = updatedApplication.individuals.headOption.value

    updatedApplication.failures.value.size shouldBe 0
    updatedApplication.status shouldBe ApplicationForRiskingStatus.Approved
    updatedIndividual.failures.value.size shouldBe 0
    updatedIndividual.failures.value.outcome() shouldBe IndividualRiskingOutcome.Approved
    updatedIndividual.status shouldBe ApplicationForRiskingStatus.Approved

  "retrieveAndProcessResultsFile syncs application status to FailedFixable when results contain fixable failures" in:

    given RequestHeader = FakeRequest()

    repo.upsert(
      tdAll.llpApplicationForRisking.copy(applicationReference = ApplicationReference("ABC123456"))
    ).futureValue shouldBe () withClue "ensure there is an application for risking in mongo before http request"

    SdesProxyStubs.stubFindAvailableFiles(Seq(tdAll.sdesFileData("resultsFile01.txt"), tdAll.sdesFileData("resultsFile02.txt")))
    ObjectStoreStubs.stubDownloadMinervaFile(testAvailableFile.downloadURL, tdAll.failRecordArrayFileMatchingApp)
    ObjectStoreStubs.stubObjectStoreListObjects()
    ObjectStoreStubs.stubObjectStoreUploadFromUrl(uploadedFilePath = "agent-registration-risking/received-results-files/resultsFile02.txt")

    service.retrieveAndProcessResultsFiles.futureValue

    val updatedApplication =
      repo.findByApplicationReference(
        ApplicationReference("ABC123456")
      ).futureValue.value
    val updatedIndividual = updatedApplication.individuals.headOption.value

    updatedApplication.failures.value.size shouldBe 1
    updatedApplication.status shouldBe ApplicationForRiskingStatus.FailedFixable
    updatedIndividual.failures.value.size shouldBe 1
    updatedIndividual.status shouldBe ApplicationForRiskingStatus.FailedFixable

  "retrieveAndProcessResultsFile does not upload to object store if the file was not processed successfully" in:
    given RequestHeader = FakeRequest()

    SdesProxyStubs.stubFindAvailableFilesFailure
    val result = service.retrieveAndProcessResultsFiles
    ObjectStoreStubs.verifyObjectStoreUploadFromUrl(count = 0)

  "downloadAndParseResultFile should successfully download and parse the result file" in:
    given RequestHeader = FakeRequest()
    ObjectStoreStubs.stubDownloadMinervaFile(testAvailableFile.downloadURL)

    val result: Future[List[RiskingResultRecord]] = service.downloadAndParseRecords(testAvailableFile)
    val expected: List[RiskingResultRecord] = List(passRecord1, passRecord2)

    result.futureValue shouldBe expected

  "downloadAndParseResultFile should handle download failures correctly" in:
    given RequestHeader = FakeRequest()
    ObjectStoreStubs.stubDownloadMinervaFileFailure(testAvailableFile.downloadURL)

    val result: Future[List[RiskingResultRecord]] = service.downloadAndParseRecords(testAvailableFile)
    val exception: UpstreamErrorResponse = recoverToExceptionIf[UpstreamErrorResponse](result).futureValue

    exception.statusCode shouldBe 500
    exception.message should include("Error when downloading file")
    exception.message should include(s"failure when retrieving file at ${testAvailableFile.downloadURL}")

  "syncApplicationStatuses" - {

    given RequestHeader = FakeRequest()

    val appRef = ApplicationReference("ABC123456")
    val personRef = PersonReference("1234567890")
    val personRef2 = PersonReference("9876543210")

    def individualWith(
      ref: PersonReference = personRef,
      status: ApplicationForRiskingStatus = ApplicationForRiskingStatus.Approved,
      failures: Option[List[IndividualFailure]] = Some(List.empty)
    ) = tdAll.readyForSubmissionIndividual(Some(ref)).copy(
      status = status,
      failures = failures
    )

    def applicationWith(
      entityFailures: Option[List[EntityFailure]],
      individualStatus: ApplicationForRiskingStatus,
      individualFailures: Option[List[IndividualFailure]]
    ) = tdAll.llpApplicationForRisking.copy(
      applicationReference = appRef,
      status = ApplicationForRiskingStatus.SubmittedForRisking,
      failures = entityFailures,
      individuals = List(
        individualWith(
          ref = personRef,
          status = individualStatus,
          failures = individualFailures
        )
      )
    )

    def applicationWithMultipleIndividuals(
      entityFailures: Option[List[EntityFailure]],
      individuals: List[IndividualForRisking]
    ) = tdAll.llpApplicationForRisking.copy(
      applicationReference = appRef,
      status = ApplicationForRiskingStatus.SubmittedForRisking,
      failures = entityFailures,
      individuals = individuals
    )

    "updates application status to Approved when entity and all individuals are approved" in {
      repo.upsert(applicationWith(
        entityFailures = Some(List.empty),
        individualStatus = ApplicationForRiskingStatus.Approved,
        individualFailures = Some(List.empty)
      )).futureValue

      service.syncApplicationStatuses(List(passRecord1, passRecord2)).futureValue

      val updated = repo.findByApplicationReference(appRef).futureValue.value
      updated.status shouldBe ApplicationForRiskingStatus.Approved
    }

    "updates application status to FailedFixable when entity has fixable failures" in {
      repo.upsert(applicationWith(
        entityFailures = Some(List(EntityFailure._3._2)),
        individualStatus = ApplicationForRiskingStatus.Approved,
        individualFailures = Some(List.empty)
      )).futureValue

      service.syncApplicationStatuses(List(passRecord1, passRecord2)).futureValue

      val updated = repo.findByApplicationReference(appRef).futureValue.value
      updated.status shouldBe ApplicationForRiskingStatus.FailedFixable
    }

    "updates application status to FailedNonFixable when any status is FailedNonFixable" in {
      repo.upsert(applicationWith(
        entityFailures = Some(List.empty),
        individualStatus = ApplicationForRiskingStatus.FailedNonFixable,
        individualFailures = Some(List(IndividualFailure._9))
      )).futureValue

      service.syncApplicationStatuses(List(passRecord1, passRecord2)).futureValue

      val updated = repo.findByApplicationReference(appRef).futureValue.value
      updated.status shouldBe ApplicationForRiskingStatus.FailedNonFixable
    }

    "updates application status to FailedFixable when individual has fixable failures" in {
      repo.upsert(applicationWith(
        entityFailures = Some(List.empty),
        individualStatus = ApplicationForRiskingStatus.FailedFixable,
        individualFailures = Some(List(IndividualFailure._4._1))
      )).futureValue

      service.syncApplicationStatuses(List(passRecord1, passRecord2)).futureValue

      val updated = repo.findByApplicationReference(appRef).futureValue.value
      updated.status shouldBe ApplicationForRiskingStatus.FailedFixable
    }

    "updates application status to FailedNonFixable when individual has non-fixable failures even if entity is approved" in {
      repo.upsert(applicationWith(
        entityFailures = Some(List.empty),
        individualStatus = ApplicationForRiskingStatus.FailedNonFixable,
        individualFailures = Some(List(IndividualFailure._8._1))
      )).futureValue

      service.syncApplicationStatuses(List(passRecord1, passRecord2)).futureValue

      val updated = repo.findByApplicationReference(appRef).futureValue.value
      updated.status shouldBe ApplicationForRiskingStatus.FailedNonFixable
    }

    "does not update application status when entity failures are not yet available" in {
      repo.upsert(applicationWith(
        entityFailures = None,
        individualStatus = ApplicationForRiskingStatus.Approved,
        individualFailures = Some(List.empty)
      )).futureValue

      service.syncApplicationStatuses(List(passRecord1, passRecord2)).futureValue

      val updated = repo.findByApplicationReference(appRef).futureValue.value
      updated.status shouldBe ApplicationForRiskingStatus.SubmittedForRisking
    }

    "does not update application status when individual status is not yet completed" in {
      repo.upsert(applicationWith(
        entityFailures = Some(List.empty),
        individualStatus = ApplicationForRiskingStatus.ReadyForSubmission,
        individualFailures = None
      )).futureValue

      service.syncApplicationStatuses(List(passRecord1, passRecord2)).futureValue

      val updated = repo.findByApplicationReference(appRef).futureValue.value
      updated.status shouldBe ApplicationForRiskingStatus.SubmittedForRisking
    }

    val passRecord2ndIndividual = RiskingResultRecord(
      recordType = "Individual",
      applicationReference = None,
      failures = Some(List.empty),
      personReference = Some(personRef2)
    )

    "does not update application status when one of multiple individuals is not yet completed" in {
      repo.upsert(applicationWithMultipleIndividuals(
        entityFailures = Some(List.empty),
        individuals = List(
          individualWith(
            ref = personRef,
            status = ApplicationForRiskingStatus.Approved,
            failures = Some(List.empty)
          ),
          individualWith(
            ref = personRef2,
            status = ApplicationForRiskingStatus.ReadyForSubmission,
            failures = None
          )
        )
      )).futureValue

      service.syncApplicationStatuses(List(
        passRecord1,
        passRecord2,
        passRecord2ndIndividual
      )).futureValue

      val updated = repo.findByApplicationReference(appRef).futureValue.value
      updated.status shouldBe ApplicationForRiskingStatus.SubmittedForRisking
    }

    "updates application status when all multiple individuals are completed" in {
      repo.upsert(applicationWithMultipleIndividuals(
        entityFailures = Some(List.empty),
        individuals = List(
          individualWith(
            ref = personRef,
            status = ApplicationForRiskingStatus.Approved,
            failures = Some(List.empty)
          ),
          individualWith(
            ref = personRef2,
            status = ApplicationForRiskingStatus.Approved,
            failures = Some(List.empty)
          )
        )
      )).futureValue

      service.syncApplicationStatuses(List(
        passRecord1,
        passRecord2,
        passRecord2ndIndividual
      )).futureValue

      val updated = repo.findByApplicationReference(appRef).futureValue.value
      updated.status shouldBe ApplicationForRiskingStatus.Approved
    }

    "updates application status to FailedFixable when multiple individuals have mixed outcomes" in {
      repo.upsert(applicationWithMultipleIndividuals(
        entityFailures = Some(List.empty),
        individuals = List(
          individualWith(
            ref = personRef,
            status = ApplicationForRiskingStatus.Approved,
            failures = Some(List.empty)
          ),
          individualWith(
            ref = personRef2,
            status = ApplicationForRiskingStatus.FailedFixable,
            failures = Some(List(IndividualFailure._4._1))
          )
        )
      )).futureValue

      service.syncApplicationStatuses(List(
        passRecord1,
        passRecord2,
        passRecord2ndIndividual
      )).futureValue

      val updated = repo.findByApplicationReference(appRef).futureValue.value
      updated.status shouldBe ApplicationForRiskingStatus.FailedFixable
    }
  }
