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

  override protected def configOverrides: Map[String, Any] = Map(
    "features.fixable-failures" -> true
  )

  private val backendNotificationService: BackendNotificationService = app.injector.instanceOf[BackendNotificationService]
  private val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  private val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  private given RequestHeader = tdAll.fakeBackendRequest

  private val approvedAfterEmailSent = TdRiskingInstancesInStates.approvedAfterEmailSent
  private val failedNonFixableAfterEmailSent = TdRiskingInstancesInStates.failedNonFixableAfterEmailSent
  private val approvedAfterOutcome = TdRiskingInstancesInStates.approvedAfterOutcome
  private val failedFixableAfterOutcome = TdRiskingInstancesInStates.failedFixableAfterOutcome
  private val failedNonFixableAfterOutcome = TdRiskingInstancesInStates.failedNonFixableAfterOutcome
  private val outcomeNotComputed = TdRiskingInstancesInStates.approved // entityRiskingResult received, but riskingOutcome not yet computed
  private val emailsNotYetSent = TdRiskingInstancesInStates.approvedAfterSubscribed // outcome + subscribed, but emailSentAt still None so notify predicate excludes it

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
      riskingCompletedDate = applicationWithIndividuals.application.overallStatus.emailSentAt.value.atZone(ZoneOffset.UTC).toLocalDate,
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

  private def backendNotifiedOf(td: TdApplicationWithIndividuals): Boolean = persisted(td).overallStatus.backendNotified

  private def persisted(td: TdApplicationWithIndividuals): ApplicationForRisking =
    applicationForRiskingRepo.findById(td.application.applicationReference).futureValue.value

  "processBackendNotifications" - {

    "notifies the backend for each application whose emails have been sent (emailSentAt exists) and has not yet been notified, then marks it as notified" in:
    "notifies the backend for each application that has a computed riskingOutcome and has not yet been notified, then marks it as notified" in:
      stubExpectedRequests(
        approvedAfterEmailSent,
        failedNonFixableAfterEmailSent
        approvedAfterOutcome,
        failedFixableAfterOutcome,
        failedNonFixableAfterOutcome
      )
      insertApplicationsWithIndividuals(
        approvedAfterEmailSent,
        failedNonFixableAfterEmailSent
        approvedAfterOutcome,
        failedFixableAfterOutcome,
        failedNonFixableAfterOutcome
      )

      backendNotificationService.processBackendNotifications().futureValue

      AgentRegistrationStubs.verifySendRiskingOutcome(approvedAfterEmailSent.application.applicationReference)
      AgentRegistrationStubs.verifySendRiskingOutcome(failedNonFixableAfterEmailSent.application.applicationReference)
      AgentRegistrationStubs.verifySendRiskingOutcome(approvedAfterOutcome.application.applicationReference)
      AgentRegistrationStubs.verifySendRiskingOutcome(failedFixableAfterOutcome.application.applicationReference)
      AgentRegistrationStubs.verifySendRiskingOutcome(failedNonFixableAfterOutcome.application.applicationReference)

      backendNotifiedOf(approvedAfterEmailSent) shouldBe true
      backendNotifiedOf(failedNonFixableAfterEmailSent) shouldBe true
      backendNotifiedOf(approvedAfterOutcome) shouldBe true
      backendNotifiedOf(failedFixableAfterOutcome) shouldBe true
      backendNotifiedOf(failedNonFixableAfterOutcome) shouldBe true

    "does not notify the backend for applications whose outcome has not been computed yet" in:
      insertApplicationsWithIndividuals(outcomeNotComputed)

      backendNotificationService.processBackendNotifications().futureValue

      AgentRegistrationStubs.verifySendRiskingOutcome(outcomeNotComputed.application.applicationReference, count = 0)
      backendNotifiedOf(outcomeNotComputed) shouldBe false

    "does not notify the backend for applications whose emails have not yet been sent (emailSentAt not set) — defers FailedFixable naturally and gates every outcome on the email step" in:
      insertApplicationsWithIndividuals(emailsNotYetSent)

      backendNotificationService.processBackendNotifications().futureValue

      AgentRegistrationStubs.verifySendRiskingOutcome(emailsNotYetSent.application.applicationReference, count = 0)
      backendNotifiedOf(emailsNotYetSent) shouldBe false

    "does not notify the backend for applications already notified" in:
      val approvedAfterBackendNotified = TdRiskingInstancesInStates.approvedAfterBackendNotified
      insertApplicationsWithIndividuals(approvedAfterBackendNotified)

      backendNotificationService.processBackendNotifications().futureValue

      AgentRegistrationStubs.verifySendRiskingOutcome(approvedAfterBackendNotified.application.applicationReference, count = 0)
      backendNotifiedOf(approvedAfterBackendNotified) shouldBe true withClue "still notified — no change"

    "leaves backendNotified unset when the backend call fails so the next run retries" in:
      AgentRegistrationStubs.stubSendRiskingOutcomeFailure(
        approvedAfterEmailSent.application.applicationReference,
        expectedRiskingOutcomeRequest(approvedAfterEmailSent)
      )
      insertApplicationsWithIndividuals(approvedAfterEmailSent)

      backendNotificationService.processBackendNotifications().futureValue

      AgentRegistrationStubs.verifySendRiskingOutcome(approvedAfterEmailSent.application.applicationReference)
      backendNotifiedOf(approvedAfterEmailSent) shouldBe false withClue "flag stays unset so the next file-ready notification retries"

    "continues notifying the rest of the batch when one application's backend call fails — fail-continue via processAllInSequence" in:
      stubExpectedRequests(approvedAfterEmailSent)
      AgentRegistrationStubs.stubSendRiskingOutcomeFailure(
        failedNonFixableAfterEmailSent.application.applicationReference,
        expectedRiskingOutcomeRequest(failedNonFixableAfterEmailSent)
      )
      insertApplicationsWithIndividuals(
        approvedAfterEmailSent,
        failedNonFixableAfterEmailSent
        approvedAfterOutcome.application.applicationReference,
        expectedRiskingOutcomeRequest(approvedAfterOutcome)
      )
      insertApplicationsWithIndividuals(approvedAfterOutcome)

      backendNotificationService.processBackendNotifications().futureValue

      AgentRegistrationStubs.verifySendRiskingOutcome(approvedAfterEmailSent.application.applicationReference)
      AgentRegistrationStubs.verifySendRiskingOutcome(failedNonFixableAfterEmailSent.application.applicationReference)
      backendNotifiedOf(approvedAfterEmailSent) shouldBe true withClue "the 202 call succeeded — its flag flips"
      backendNotifiedOf(failedNonFixableAfterEmailSent) shouldBe false withClue "the 500 call failed — its flag must not flip so the next scheduler run retries"
      AgentRegistrationStubs.verifySendRiskingOutcome(approvedAfterOutcome.application.applicationReference)
      backendNotifiedOf(approvedAfterOutcome) shouldBe false withClue "flag stays unset so the next file-ready notification retries"

    "notifies the backend for legacy applications persisted before the backendNotified field was added (field missing on the doc)" in:
      stubExpectedRequests(approvedAfterEmailSent)
      insertApplicationsWithIndividuals(approvedAfterEmailSent)
      stubExpectedRequests(approvedAfterOutcome)
      insertApplicationsWithIndividuals(approvedAfterOutcome)
      // Simulate legacy doc: remove the overallStatus.backendNotified field from the persisted record
      applicationForRiskingRepo.collection
        .updateOne(
          Filters.eq("applicationReference", approvedAfterEmailSent.application.applicationReference.value),
          Filters.eq("applicationReference", approvedAfterOutcome.application.applicationReference.value),
          Updates.unset("overallStatus.backendNotified")
        )
        .toFuture
        .futureValue

      backendNotificationService.processBackendNotifications().futureValue

      AgentRegistrationStubs.verifySendRiskingOutcome(
        approvedAfterEmailSent.application.applicationReference
        approvedAfterOutcome.application.applicationReference
      ) withClue "legacy doc (missing backendNotified) should be picked up and notified"
      backendNotifiedOf(approvedAfterEmailSent) shouldBe true withClue "after notify, backendNotified should be set"
      backendNotifiedOf(approvedAfterOutcome) shouldBe true withClue "after notify, backendNotified should be set"
  }
