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
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailures
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcome
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

class BackendNotificationServiceFeatureFlagSpec
extends ISpec:

  override protected def configOverrides: Map[String, Any] = Map(
    "features.fixable-failures" -> false
  )

  private val backendNotificationService: BackendNotificationService = app.injector.instanceOf[BackendNotificationService]
  private val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  private val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  private given RequestHeader = tdAll.fakeBackendRequest

  private val approvedAfterEmailSent = TdRiskingInstancesInStates.approvedAfterEmailSent
  private val failedNonFixableAfterEmailSent = TdRiskingInstancesInStates.failedNonFixableAfterEmailSent

  // Constructed inline because `failedFixableAfterEmailSent` case object is commented out until the FailedFixable email service ships.
  // Predicate `emailSentAt exists` would otherwise let it through — this spec proves the fixable-failures flag holds as an additional gate on top of the predicate.
  private val failedFixableAfterEmailSentApp: ApplicationForRisking = TdRiskingInstancesInStates.failedFixableAfterOutcome.application
    .copy(isEmailSent = true)
    .modify(_.overallStatus.emailsProcessed).setTo(true)
    .modify(_.overallStatus.emailSentAt).setTo(Some(frozenInstant))
  private val failedFixableAfterEmailSent: TdApplicationWithIndividuals =
    new TdApplicationWithIndividuals:
      override def tdRisking = TdRiskingInstancesInStates.failedFixableAfterOutcome.tdRisking
      override val application = failedFixableAfterEmailSentApp
      override val individual1 = TdRiskingInstancesInStates.failedFixableAfterOutcome.individual1
      override val individual2 = TdRiskingInstancesInStates.failedFixableAfterOutcome.individual2
      override val riskingProgressForApplicant = TdRiskingInstancesInStates.failedFixableAfterOutcome.riskingProgressForApplicant

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

  "doesn't notify the backend for FailedFixable applications when the fixable-failures flag is OFF — even if emailSentAt is set the flag holds as an additional gate on top of the predicate" in:
    stubExpectedRequests(failedFixableAfterEmailSent)
    insertApplicationsWithIndividuals(failedFixableAfterEmailSent)

    backendNotificationService.processBackendNotifications().futureValue

    AgentRegistrationStubs.verifySendRiskingOutcome(failedFixableAfterEmailSent.application.applicationReference, count = 0)
    backendNotifiedOf(failedFixableAfterEmailSent) shouldBe false

  "notifies the backend for Approved and FailedNonFixable outcomes when the fixable-failures flag is OFF — the flag only gates FailedFixable" in:
    stubExpectedRequests(
      approvedAfterEmailSent,
      failedNonFixableAfterEmailSent
    )
    insertApplicationsWithIndividuals(
      approvedAfterEmailSent,
      failedNonFixableAfterEmailSent
    )

    backendNotificationService.processBackendNotifications().futureValue

    AgentRegistrationStubs.verifySendRiskingOutcome(approvedAfterEmailSent.application.applicationReference)
    AgentRegistrationStubs.verifySendRiskingOutcome(failedNonFixableAfterEmailSent.application.applicationReference)

    backendNotifiedOf(approvedAfterEmailSent) shouldBe true
    backendNotifiedOf(failedNonFixableAfterEmailSent) shouldBe true
