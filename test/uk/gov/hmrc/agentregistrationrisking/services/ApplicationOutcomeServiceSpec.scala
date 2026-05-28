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

import org.mongodb.scala.SingleObservableFuture
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.RiskingOutcome
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.AuditStubs

import java.time.Duration
import java.time.Instant

class ApplicationOutcomeServiceSpec
extends ISpec:

  // `auditing.enabled` defaults to false in ISpec, which makes DefaultAuditConnector short-circuit and post nothing.
  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "auditing.enabled" -> true
  )

  private val applicationOutcomeService: ApplicationOutcomeService = app.injector.instanceOf[ApplicationOutcomeService]
  private val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  private val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  private given RequestHeader = tdAll.fakeBackendRequest

  private val approved = TdRiskingInstancesInStates.approved
  private val failedFixable = TdRiskingInstancesInStates.failedFixable
  private val failedNonFixable = TdRiskingInstancesInStates.failedNonFixable

  // submitted for risking but no entity result yet — must NOT be picked up.
  private val notReady = TdRiskingInstancesInStates.submittedForRisking

  override def beforeEach(): Unit =
    super.beforeEach()
    applicationForRiskingRepo.collection.drop().toFuture.futureValue
    individualForRiskingRepo.collection.drop().toFuture.futureValue
    ()

  private def insertApplicationsWithIndividuals(tds: TdApplicationWithIndividuals*): Unit = tds.foreach: td =>
    applicationForRiskingRepo.upsert(td.application).futureValue
    individualForRiskingRepo.upsert(td.individual1).futureValue
    individualForRiskingRepo.upsert(td.individual2).futureValue

  "processOverallOutcomes" - {

    "sends a RiskingDetermination audit event and persists the computed outcome for each ready application" in:
      AuditStubs.stubAuditWrite()
      insertApplicationsWithIndividuals(
        approved,
        failedFixable,
        failedNonFixable
      )

      applicationOutcomeService.processOverallOutcomes().futureValue

      eventually:
        AuditStubs.verifyAuditSent(
          auditType = "RiskingDetermination",
          detail = Json.obj("applicationReference" -> approved.application.applicationReference.value, "determination" -> "Success")
        )
        AuditStubs.verifyAuditSent(
          auditType = "RiskingDetermination",
          detail = Json.obj("applicationReference" -> failedFixable.application.applicationReference.value, "determination" -> "FixableFailure")
        )
        AuditStubs.verifyAuditSent(
          auditType = "RiskingDetermination",
          detail = Json.obj("applicationReference" -> failedNonFixable.application.applicationReference.value, "determination" -> "NonFixableFailure")
        )

      outcomeOf(approved) shouldBe Some(RiskingOutcome.Approved)
      outcomeOf(failedFixable) shouldBe Some(RiskingOutcome.FailedFixable)
      outcomeOf(failedNonFixable) shouldBe Some(RiskingOutcome.FailedNonFixable)

    "sets correctiveActionExpiryDate to (now + 45 days) when the computed outcome is FailedFixable" in:
      AuditStubs.stubAuditWrite()
      insertApplicationsWithIndividuals(failedFixable)

      applicationOutcomeService.processOverallOutcomes().futureValue

      correctiveActionExpiryDateOf(failedFixable) shouldBe Some(frozenInstant.plus(Duration.ofDays(45)))

    "sets correctiveActionExpiryDate to (now + 45 days) when the computed outcome is FailedNonFixable" in:
      AuditStubs.stubAuditWrite()
      insertApplicationsWithIndividuals(failedNonFixable)

      applicationOutcomeService.processOverallOutcomes().futureValue

      correctiveActionExpiryDateOf(failedNonFixable) shouldBe Some(frozenInstant.plus(Duration.ofDays(45)))

    "leaves correctiveActionExpiryDate unset when the computed outcome is Approved" in:
      AuditStubs.stubAuditWrite()
      insertApplicationsWithIndividuals(approved)

      applicationOutcomeService.processOverallOutcomes().futureValue

      correctiveActionExpiryDateOf(approved) shouldBe None

    "does not set outcome or correctiveActionExpiryDate when only some individuals have results yet" in:
      AuditStubs.stubAuditWrite()
      val partiallyRisked = TdRiskingInstancesInStates.partiallyRisked.failedNonFixable_failedFixable_submitted
      insertApplicationsWithIndividuals(partiallyRisked)

      applicationOutcomeService.processOverallOutcomes().futureValue

      outcomeOf(partiallyRisked) shouldBe None
      correctiveActionExpiryDateOf(partiallyRisked) shouldBe None

    "does not send a RiskingDetermination or set an outcome for an application that is not ready" in:
      AuditStubs.stubAuditWrite()
      insertApplicationsWithIndividuals(failedNonFixable, notReady)

      applicationOutcomeService.processOverallOutcomes().futureValue

      eventually:
        AuditStubs.verifyAuditSent(
          auditType = "RiskingDetermination",
          detail = Json.obj("applicationReference" -> failedNonFixable.application.applicationReference.value, "determination" -> "NonFixableFailure")
        )

      AuditStubs.verifyAuditSent(
        auditType = "RiskingDetermination",
        detail = Json.obj("applicationReference" -> notReady.application.applicationReference.value, "determination" -> "Success"),
        count = 0
      )
      outcomeOf(notReady) shouldBe None
  }

  private def outcomeOf(td: TdApplicationWithIndividuals): Option[RiskingOutcome] =
    persisted(td)
      .overallStatus
      .riskingOutcome

  private def correctiveActionExpiryDateOf(td: TdApplicationWithIndividuals): Option[Instant] =
    persisted(td)
      .correctiveActionExpiryDate

  private def persisted(td: TdApplicationWithIndividuals): ApplicationForRisking =
    applicationForRiskingRepo
      .findById(td.application.applicationReference)
      .futureValue
      .value
