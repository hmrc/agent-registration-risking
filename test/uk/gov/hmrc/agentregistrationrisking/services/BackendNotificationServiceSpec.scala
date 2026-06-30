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
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Updates
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailures
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeRequest
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.services.RiskingOutcomeHelper.outcome
import uk.gov.hmrc.agentregistrationrisking.services.RiskingOutcomeHelper.outcomeForEntity
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.AgentRegistrationStubs

import java.time.ZoneOffset

class BackendNotificationServiceSpec
extends ISpec:

  private val backendNotificationService: BackendNotificationService = app.injector.instanceOf[BackendNotificationService]
  private val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  private val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  private given RequestHeader = tdAll.fakeBackendRequest

  private val approvedAfterEmailsProcessed = TdRiskingInstancesInStates.approvedAfterEmailsProcessed
  private val failedFixableAfterOutcome = TdRiskingInstancesInStates.failedFixableAfterOutcome
  private val failedNonFixableAfterAllEmailsProcessed = TdRiskingInstancesInStates.failedNonFixableAfterAllEmailsProcessed
  private val outcomeNotComputed = TdRiskingInstancesInStates.approved // entityRiskingResult received, but riskingOutcome not yet computed

  override def beforeEach(): Unit =
    super.beforeEach()
    applicationForRiskingRepo.collection.drop().toFuture.futureValue
    individualForRiskingRepo.collection.drop().toFuture.futureValue
    ()

  private def insertApplicationsWithIndividuals(tds: TdApplicationWithIndividuals*): Unit = tds.foreach: td =>
    applicationForRiskingRepo.upsert(td.application).futureValue
    individualForRiskingRepo.upsert(td.individual1).futureValue
    individualForRiskingRepo.upsert(td.individual2).futureValue

  private def expectedRiskingOutcomeRequest(td: TdApplicationWithIndividuals): RiskingOutcomeRequest =
    val applicationWithIndividuals = td.applicationWithIndividuals
    val entityFailures = applicationWithIndividuals.application.entityRiskingResult.map(_.failures).getOrElse(List.empty)
    RiskingOutcomeRequest(
      riskingCompletedDate = applicationWithIndividuals.riskingCompletedDate.value.atZone(ZoneOffset.UTC).toLocalDate,
      applicationOutcome = applicationWithIndividuals.application.overallStatus.riskingOutcome.value,
      entityFailures = entityFailures,
      entityOutcome = entityFailures.outcomeForEntity,
      individualFailures = applicationWithIndividuals.individuals.map: individual =>
        val individualFailureList = individual.individualRiskingResult.map(_.failures).getOrElse(List.empty)
        IndividualFailures(
          personReference = individual.individualData.personReference,
          failures = individualFailureList,
          riskingOutcome = individualFailureList.outcome
        )
    )

  private def stubExpectedRequests(tds: TdApplicationWithIndividuals*): Unit = tds.foreach: td =>
    AgentRegistrationStubs.stubSendRiskingOutcome(td.application.applicationReference, expectedRiskingOutcomeRequest(td))

  private def backendNotifiedOf(td: TdApplicationWithIndividuals): Boolean = persisted(td).overallStatus.backendNotified.contains(true)

  private def persisted(td: TdApplicationWithIndividuals): ApplicationForRisking =
    applicationForRiskingRepo.findById(td.application.applicationReference).futureValue.value

  "processBackendNotifications" - {

    "notifies the backend for each application that has a computed riskingOutcome and has not yet been notified, then marks it as notified" in:
      stubExpectedRequests(
        approvedAfterEmailsProcessed,
        failedFixableAfterOutcome,
        failedNonFixableAfterAllEmailsProcessed
      )
      insertApplicationsWithIndividuals(
        approvedAfterEmailsProcessed,
        failedFixableAfterOutcome,
        failedNonFixableAfterAllEmailsProcessed
      )

      backendNotificationService.processBackendNotifications().futureValue

      AgentRegistrationStubs.verifySendRiskingOutcome(approvedAfterEmailsProcessed.application.applicationReference)
      AgentRegistrationStubs.verifySendRiskingOutcome(failedFixableAfterOutcome.application.applicationReference)
      AgentRegistrationStubs.verifySendRiskingOutcome(failedNonFixableAfterAllEmailsProcessed.application.applicationReference)

      backendNotifiedOf(approvedAfterEmailsProcessed) shouldBe true
      backendNotifiedOf(failedFixableAfterOutcome) shouldBe true
      backendNotifiedOf(failedNonFixableAfterAllEmailsProcessed) shouldBe true

    "does not notify the backend for applications whose outcome has not been computed yet" in:
      insertApplicationsWithIndividuals(outcomeNotComputed)

      backendNotificationService.processBackendNotifications().futureValue

      AgentRegistrationStubs.verifySendRiskingOutcome(outcomeNotComputed.application.applicationReference, count = 0)
      backendNotifiedOf(outcomeNotComputed) shouldBe false

    "does not notify the backend for applications already notified" in:
      val alreadyNotified: ApplicationForRisking = approvedAfterEmailsProcessed
        .application
        .modify(_.overallStatus.backendNotified)
        .setTo(Some(true))
      applicationForRiskingRepo.upsert(alreadyNotified).futureValue
      individualForRiskingRepo.upsert(approvedAfterEmailsProcessed.individual1).futureValue
      individualForRiskingRepo.upsert(approvedAfterEmailsProcessed.individual2).futureValue

      backendNotificationService.processBackendNotifications().futureValue

      AgentRegistrationStubs.verifySendRiskingOutcome(approvedAfterEmailsProcessed.application.applicationReference, count = 0)
      backendNotifiedOf(approvedAfterEmailsProcessed) shouldBe true withClue "still notified — no change"

    "leaves backendNotified unset when the backend call fails so the next run retries" in:
      AgentRegistrationStubs.stubSendRiskingOutcomeFailure(
        approvedAfterEmailsProcessed.application.applicationReference,
        expectedRiskingOutcomeRequest(approvedAfterEmailsProcessed)
      )
      insertApplicationsWithIndividuals(approvedAfterEmailsProcessed)

      backendNotificationService.processBackendNotifications().futureValue

      AgentRegistrationStubs.verifySendRiskingOutcome(approvedAfterEmailsProcessed.application.applicationReference)
      backendNotifiedOf(approvedAfterEmailsProcessed) shouldBe false withClue "flag stays unset so the next file-ready notification retries"

    "does not notify the backend for applications missing entityRiskingResult (data inconsistency — would otherwise loop forever)" in:
      insertApplicationsWithIndividuals(approvedAfterEmailsProcessed)
      applicationForRiskingRepo.collection
        .updateOne(
          Filters.eq("applicationReference", approvedAfterEmailsProcessed.application.applicationReference.value),
          Updates.unset("entityRiskingResult")
        )
        .toFuture
        .futureValue

      backendNotificationService.processBackendNotifications().futureValue

      AgentRegistrationStubs.verifySendRiskingOutcome(
        approvedAfterEmailsProcessed.application.applicationReference,
        count = 0
      ) withClue "missing entityRiskingResult → record must be excluded by query, not allowed to reach process() and loop"

    "does not notify the backend for applications where any individual is missing individualRiskingResult (data inconsistency — would otherwise loop forever)" in:
      insertApplicationsWithIndividuals(approvedAfterEmailsProcessed)
      individualForRiskingRepo.collection
        .updateOne(
          Filters.eq("personReference", approvedAfterEmailsProcessed.individual2.personReference.value),
          Updates.unset("individualRiskingResult")
        )
        .toFuture
        .futureValue

      backendNotificationService.processBackendNotifications().futureValue

      AgentRegistrationStubs.verifySendRiskingOutcome(
        approvedAfterEmailsProcessed.application.applicationReference,
        count = 0
      ) withClue "individual2 missing individualRiskingResult → record must be excluded by query, not allowed to reach process() and loop"

    "notifies the backend for legacy applications persisted before the backendNotified field was added (field missing on the doc)" in:
      stubExpectedRequests(approvedAfterEmailsProcessed)
      insertApplicationsWithIndividuals(approvedAfterEmailsProcessed)
      // Simulate legacy doc: remove the overallStatus.backendNotified field from the persisted record
      applicationForRiskingRepo.collection
        .updateOne(
          Filters.eq("applicationReference", approvedAfterEmailsProcessed.application.applicationReference.value),
          Updates.unset("overallStatus.backendNotified")
        )
        .toFuture
        .futureValue

      backendNotificationService.processBackendNotifications().futureValue

      AgentRegistrationStubs.verifySendRiskingOutcome(
        approvedAfterEmailsProcessed.application.applicationReference
      ) withClue "legacy doc (missing backendNotified) should be picked up and notified"
      backendNotifiedOf(approvedAfterEmailsProcessed) shouldBe true withClue "after notify, backendNotified should be set"
  }
