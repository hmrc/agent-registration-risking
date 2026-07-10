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

import com.softwaremill.quicklens.modify
import org.mongodb.scala.ObservableFuture
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcome
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.CompletedRisking
import uk.gov.hmrc.agentregistrationrisking.model.CompletedRiskingId
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFile
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.CompletedRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.FieldNames
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.RiskingFileRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdCompletedRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates

class RiskingArchivalServiceSpec
extends ISpec:

  private val riskingArchivalService: RiskingArchivalService = app.injector.instanceOf[RiskingArchivalService]
  private val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  private val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]
  private val completedRiskingRepo: CompletedRiskingRepo = app.injector.instanceOf[CompletedRiskingRepo]
  private val riskingFileRepo: RiskingFileRepo = app.injector.instanceOf[RiskingFileRepo]

  private given RequestHeader = tdAll.fakeBackendRequest

  private val approvedAfterBackendNotified = TdRiskingInstancesInStates.approvedAfterBackendNotified
  private val failedFixableAfterBackendNotified = TdRiskingInstancesInStates.failedFixableAfterBackendNotified
  private val failedNonFixableAfterBackendNotified = TdRiskingInstancesInStates.failedNonFixableAfterBackendNotified
  private val notReadyYet = TdRiskingInstancesInStates.approvedAfterOutcome

  override def beforeEach(): Unit =
    super.beforeEach()
    applicationForRiskingRepo.collection.drop().toFuture.futureValue
    individualForRiskingRepo.collection.drop().toFuture.futureValue
    completedRiskingRepo.collection.drop().toFuture.futureValue
    riskingFileRepo.collection.drop().toFuture.futureValue
    ()

  private def insertApplicationsWithIndividuals(tds: TdApplicationWithIndividuals*): Unit = tds.foreach: td =>
    insertOne(
      td.application,
      td.individual1,
      td.individual2,
      td.tdRisking.riskingFile
    )

  private def insertOne(
    application: ApplicationForRisking,
    individual1: IndividualForRisking,
    individual2: IndividualForRisking,
    riskingFile: RiskingFile
  ): Unit =
    applicationForRiskingRepo.upsert(application).futureValue
    individualForRiskingRepo.upsert(individual1).futureValue
    individualForRiskingRepo.upsert(individual2).futureValue
    riskingFileRepo.upsert(riskingFile).futureValue

  private def expectedCompletedRisking(
    td: TdApplicationWithIndividuals,
    generatedId: CompletedRiskingId
  ): CompletedRisking = TdCompletedRisking.makeCompletedRisking(
    completedRiskingId = generatedId,
    completedAt = frozenInstant,
    riskingFile = td.tdRisking.riskingFile,
    application = td.application,
    individuals = Seq(td.individual1, td.individual2)
  )

  "processArchivals" - {

    "archives each ready application (Approved, FailedFixable, FailedNonFixable) as a CompletedRisking record, then removes the originals from operational collections" in:
      insertApplicationsWithIndividuals(
        approvedAfterBackendNotified,
        failedFixableAfterBackendNotified,
        failedNonFixableAfterBackendNotified
      )

      riskingArchivalService.processArchivals().futureValue

      val archivedApproved: CompletedRisking = completedRiskingRepo.findRecent(approvedAfterBackendNotified.application.applicationReference).futureValue.value
      archivedApproved shouldBe expectedCompletedRisking(approvedAfterBackendNotified, archivedApproved._id)

      val archivedFixable: CompletedRisking =
        completedRiskingRepo.findRecent(failedFixableAfterBackendNotified.application.applicationReference).futureValue.value
      archivedFixable shouldBe expectedCompletedRisking(failedFixableAfterBackendNotified, archivedFixable._id)

      val archivedNonFixable: CompletedRisking =
        completedRiskingRepo.findRecent(failedNonFixableAfterBackendNotified.application.applicationReference).futureValue.value
      archivedNonFixable shouldBe expectedCompletedRisking(failedNonFixableAfterBackendNotified, archivedNonFixable._id)

      applicationForRiskingRepo.findById(
        approvedAfterBackendNotified.application.applicationReference
      ).futureValue shouldBe None withClue "operational application removed"
      individualForRiskingRepo.findByApplicationReference(
        approvedAfterBackendNotified.application.applicationReference
      ).futureValue shouldBe empty withClue "operational individuals removed"
      applicationForRiskingRepo.findById(
        failedFixableAfterBackendNotified.application.applicationReference
      ).futureValue shouldBe None
      individualForRiskingRepo.findByApplicationReference(
        failedFixableAfterBackendNotified.application.applicationReference
      ).futureValue shouldBe empty
      applicationForRiskingRepo.findById(
        failedNonFixableAfterBackendNotified.application.applicationReference
      ).futureValue shouldBe None
      individualForRiskingRepo.findByApplicationReference(
        failedNonFixableAfterBackendNotified.application.applicationReference
      ).futureValue shouldBe empty

    "does not archive Approved applications with emailsProcessed=true but isSubscribed=false — proves the Approved isSubscribed gate is load-bearing (synthetic state — this combination is unreachable in production, but guards the predicate against silent gate-drop regressions)" in:
      val emailsProcessedButNotSubscribed: ApplicationForRisking = approvedAfterBackendNotified
        .application
        .copy(isSubscribed = false)
      insertOne(
        application = emailsProcessedButNotSubscribed,
        individual1 = approvedAfterBackendNotified.individual1,
        individual2 = approvedAfterBackendNotified.individual2,
        riskingFile = approvedAfterBackendNotified.tdRisking.riskingFile
      )

      riskingArchivalService.processArchivals().futureValue

      completedRiskingRepo.findRecent(emailsProcessedButNotSubscribed.applicationReference).futureValue shouldBe None withClue
        "Approved branch of findReadyToArchive predicate must require isSubscribed=true; if this gate is silently dropped, the record would archive despite subscription never completing"
      applicationForRiskingRepo.findById(
        emailsProcessedButNotSubscribed.applicationReference
      ).futureValue.value shouldBe emailsProcessedButNotSubscribed withClue "operational application untouched"

    "does not archive when emailsProcessed=false (even if backendNotified=true and outcome set) — proves the common emailsProcessed=true gate is load-bearing at the service layer" in:
      val notEmailsProcessed: ApplicationForRisking = approvedAfterBackendNotified
        .application
        .modify(_.overallStatus.emailsProcessed)
        .setTo(false)
      applicationForRiskingRepo.upsert(notEmailsProcessed).futureValue
      individualForRiskingRepo.upsert(approvedAfterBackendNotified.individual1).futureValue
      individualForRiskingRepo.upsert(approvedAfterBackendNotified.individual2).futureValue
      riskingFileRepo.upsert(approvedAfterBackendNotified.tdRisking.riskingFile).futureValue

      riskingArchivalService.processArchivals().futureValue

      completedRiskingRepo.findRecent(notEmailsProcessed.applicationReference).futureValue shouldBe None withClue
        "emailsProcessed=false must exclude the record regardless of other gates — if this gate silently drops, applicants get archived without their emails ever going out"
      applicationForRiskingRepo.findById(notEmailsProcessed.applicationReference).futureValue.value shouldBe notEmailsProcessed withClue "operational untouched"

    "does not archive applications whose upstream steps are not all complete" in:
      insertApplicationsWithIndividuals(notReadyYet)

      riskingArchivalService.processArchivals().futureValue

      completedRiskingRepo.findRecent(notReadyYet.application.applicationReference).futureValue shouldBe None
      applicationForRiskingRepo.findById(
        notReadyYet.application.applicationReference
      ).futureValue.value shouldBe notReadyYet.application withClue "operational application untouched"

    "quarantines a bad application (riskingFileName=None) and still archives the rest of the batch — sync throws must be caught by processAllInSequence's per-item recover" in:
      val badApp: ApplicationForRisking = failedNonFixableAfterBackendNotified
        .application
        .copy(riskingFileName = None)
      applicationForRiskingRepo.upsert(badApp).futureValue
      individualForRiskingRepo.upsert(failedNonFixableAfterBackendNotified.individual1).futureValue
      individualForRiskingRepo.upsert(failedNonFixableAfterBackendNotified.individual2).futureValue
      insertApplicationsWithIndividuals(approvedAfterBackendNotified)

      riskingArchivalService.processArchivals().futureValue

      completedRiskingRepo.findRecent(approvedAfterBackendNotified.application.applicationReference).futureValue shouldBe defined withClue
        "valid record must still be archived even though the batch also contained a bad record"
      applicationForRiskingRepo.findById(approvedAfterBackendNotified.application.applicationReference).futureValue shouldBe None

      completedRiskingRepo.findRecent(badApp.applicationReference).futureValue shouldBe None
      applicationForRiskingRepo.findById(badApp.applicationReference).futureValue.value shouldBe badApp withClue
        "bad record left in operational for retry / manual intervention"

    "archives with individuals=Seq.empty when the operational individuals are missing (TTL'd or otherwise lost) — mirrors F4 degraded-archive semantics for missing individuals; warns and does not stall the operational record" in:
      applicationForRiskingRepo.upsert(approvedAfterBackendNotified.application).futureValue
      riskingFileRepo.upsert(approvedAfterBackendNotified.tdRisking.riskingFile).futureValue

      riskingArchivalService.processArchivals().futureValue

      val archived: CompletedRisking = completedRiskingRepo.findRecent(approvedAfterBackendNotified.application.applicationReference).futureValue.value
      archived.individuals shouldBe Seq.empty withClue "degraded archive: individuals were missing so archive has an empty list"
      archived.application shouldBe approvedAfterBackendNotified.application

      applicationForRiskingRepo.findById(approvedAfterBackendNotified.application.applicationReference).futureValue shouldBe None withClue
        "operational cleaned up despite missing individuals — no infinite retry"

    "archives with riskingFile=None when the source RiskingFile row is missing from riskingFileRepo — proves F4 fix (degraded archive) prevents the infinite-retry sink" in:
      applicationForRiskingRepo.upsert(approvedAfterBackendNotified.application).futureValue
      individualForRiskingRepo.upsert(approvedAfterBackendNotified.individual1).futureValue
      individualForRiskingRepo.upsert(approvedAfterBackendNotified.individual2).futureValue

      riskingArchivalService.processArchivals().futureValue

      val archived: CompletedRisking = completedRiskingRepo.findRecent(approvedAfterBackendNotified.application.applicationReference).futureValue.value
      archived.riskingFile shouldBe None withClue "degraded archive — riskingFile is None when the source row was missing"
      archived.application shouldBe approvedAfterBackendNotified.application
      archived.individuals shouldBe Seq(approvedAfterBackendNotified.individual1, approvedAfterBackendNotified.individual2)

      applicationForRiskingRepo.findById(approvedAfterBackendNotified.application.applicationReference).futureValue shouldBe None withClue
        "operational cleaned up despite missing source file — no infinite retry"
      individualForRiskingRepo.findByApplicationReference(approvedAfterBackendNotified.application.applicationReference).futureValue shouldBe empty

    "after a crash between application-delete and individuals-delete, the next scheduler tick does NOT create a shadow archive with empty individuals — proves the reversed delete order eliminates the F3 window" in:
      val originalArchive: CompletedRisking = TdRiskingInstancesInStates.failedFixableAfterBackendNotified.completedRisking
      completedRiskingRepo.upsert(originalArchive).futureValue
      individualForRiskingRepo.upsert(failedFixableAfterBackendNotified.individual1).futureValue
      individualForRiskingRepo.upsert(failedFixableAfterBackendNotified.individual2).futureValue
      riskingFileRepo.upsert(failedFixableAfterBackendNotified.tdRisking.riskingFile).futureValue

      riskingArchivalService.processArchivals().futureValue

      val archivesForAppRef: Seq[CompletedRisking] =
        completedRiskingRepo.collection
          .find(Filters.eq(FieldNames.CompletedRisking.applicationReference, failedFixableAfterBackendNotified.application.applicationReference.value))
          .toFuture()
          .futureValue

      archivesForAppRef should have size 1 withClue "no shadow re-archive — only the pre-existing archive from before the crash remains"
      archivesForAppRef.head shouldBe originalArchive withClue "the pre-existing archive is untouched"

      individualForRiskingRepo.findByApplicationReference(
        failedFixableAfterBackendNotified.application.applicationReference
      ).futureValue should not be empty withClue
        "orphaned individuals remain in the operational collection — documenting the trade-off; manual cleanup / TTL / a future sweeper is expected to handle"

    "archives a resubmission (same applicationReference) as a separate history entry — the resubmission audit trail relies on both cycles being preserved" in:
      insertApplicationsWithIndividuals(approvedAfterBackendNotified)

      riskingArchivalService.processArchivals().futureValue

      val sharedAppRef: ApplicationReference = approvedAfterBackendNotified.application.applicationReference
      val cycle1Archive: CompletedRisking = completedRiskingRepo.findRecent(sharedAppRef).futureValue.value
      cycle1Archive.application.overallStatus.riskingOutcome shouldBe Some(RiskingOutcome.Approved)
      applicationForRiskingRepo.findById(sharedAppRef).futureValue shouldBe None withClue "operational cleaned up after cycle 1"

      val cycle2App: ApplicationForRisking = failedNonFixableAfterBackendNotified.application.copy(applicationReference = sharedAppRef)
      val cycle2Ind1: IndividualForRisking = failedNonFixableAfterBackendNotified.individual1.copy(
        applicationReference = sharedAppRef,
        personReference = approvedAfterBackendNotified.individual1.personReference
      )
      val cycle2Ind2: IndividualForRisking = failedNonFixableAfterBackendNotified.individual2.copy(
        applicationReference = sharedAppRef,
        personReference = approvedAfterBackendNotified.individual2.personReference
      )
      val cycle2RiskingFile: RiskingFile = failedNonFixableAfterBackendNotified.tdRisking.riskingFile

      applicationForRiskingRepo.upsert(cycle2App).futureValue
      individualForRiskingRepo.upsert(cycle2Ind1).futureValue
      individualForRiskingRepo.upsert(cycle2Ind2).futureValue
      riskingFileRepo.upsert(cycle2RiskingFile).futureValue

      riskingArchivalService.processArchivals().futureValue

      val allArchivesForSharedAppRef: Seq[CompletedRisking] =
        completedRiskingRepo.collection
          .find(Filters.eq(FieldNames.CompletedRisking.applicationReference, sharedAppRef.value))
          .toFuture()
          .futureValue

      allArchivesForSharedAppRef should have size 2 withClue "two separate archive entries for the same applicationReference — history-log semantics"
      allArchivesForSharedAppRef.map(_._id).toSet should have size 2 withClue "each cycle produces a distinct CompletedRiskingId"
      allArchivesForSharedAppRef.map(_.application.overallStatus.riskingOutcome).toSet shouldBe Set(
        Some(RiskingOutcome.Approved),
        Some(RiskingOutcome.FailedNonFixable)
      ) withClue "both cycles' outcomes preserved in the audit trail"
      applicationForRiskingRepo.findById(sharedAppRef).futureValue shouldBe None withClue "operational cleaned up after cycle 2"

      val latestByAppRef: CompletedRisking = completedRiskingRepo.findRecent(sharedAppRef).futureValue.value
      latestByAppRef.application.overallStatus.riskingOutcome shouldBe Some(RiskingOutcome.FailedNonFixable) withClue
        "findRecent(applicationReference) must deterministically return cycle 2 (the later archive) even under tied completedAt — F8 secondary-sort on _id"

      val sharedPersonReference = approvedAfterBackendNotified.individual1.personReference
      val latestByPersonRef: CompletedRisking = completedRiskingRepo.findRecent(sharedPersonReference).futureValue.value
      latestByPersonRef.application.overallStatus.riskingOutcome shouldBe Some(RiskingOutcome.FailedNonFixable) withClue
        "findRecent(personReference) must return the most-recent cycle for a resubmitted person — F9 real-resubmission coverage (same personReference across cycles)"
  }
