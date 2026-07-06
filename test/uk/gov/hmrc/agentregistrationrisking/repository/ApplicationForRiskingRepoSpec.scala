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

package uk.gov.hmrc.agentregistrationrisking.repository

import org.mongodb.scala.SingleObservableFuture
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcome
import uk.gov.hmrc.agentregistrationrisking.model.{ApplicationForRisking, ApplicationWithIndividuals, OverallStatus}
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates

class ApplicationForRiskingRepoSpec
extends ISpec:

  "findReadyForSubmission should return all applications which don't have riskingFileId, which means they aren't submitted to minerva yet" in:
    val applications: Seq[ApplicationWithIndividuals] =
      applicationForRiskingRepo
        .findReadyForSubmission()
        .futureValue
    applications.toSet shouldBe Set(
      TdRiskingInstancesInStates.readyForSubmission.applicationWithIndividuals,
      TdRiskingInstancesInStates.readyForSubmission2.applicationWithIndividuals
    )

  "findReadyToBeSubscribed should return all applications which are approved but not subscribed yet" in:

    val applications: Seq[ApplicationForRisking] =
      applicationForRiskingRepo
        .findReadyToBeSubscribed()
        .futureValue

    applications.toSet shouldBe Set(
      TdRiskingInstancesInStates.approvedAfterOutcome.application,
      TdRiskingInstancesInStates.approvedAfterBackendNotified.application
    ) withClue applications.map(_.applicationReference.value).mkString(", ")

  "findReadyToSetRiskingOutcome" in:

    val applications: Seq[ApplicationWithIndividuals] =
      applicationForRiskingRepo
        .findReadyToSetRiskingOutcome()
        .futureValue

    applications.toSet shouldBe Set(
      TdRiskingInstancesInStates.approved.applicationWithIndividuals,
      TdRiskingInstancesInStates.failedFixable.applicationWithIndividuals,
      TdRiskingInstancesInStates.failedNonFixable.applicationWithIndividuals
    )

  "findRequiringEmailProcessingForFailedNonFixable" in:

    val applications: Seq[ApplicationWithIndividuals] =
      applicationForRiskingRepo
        .findRequiringEmailProcessingForFailedNonFixable()
        .futureValue

    applications.toSet shouldBe Set(
      TdRiskingInstancesInStates.failedNonFixableAfterOutcome.applicationWithIndividuals,
      TdRiskingInstancesInStates.failedNonFixableAfterBackendNotified.applicationWithIndividuals,
      TdRiskingInstancesInStates.failedNonFixableAfter1EmailSent.applicationWithIndividuals,
      TdRiskingInstancesInStates.failedNonFixableAfter2EmailsSent.applicationWithIndividuals,
      TdRiskingInstancesInStates.failedNonFixableAfterAllEmailsSent.applicationWithIndividuals
    ) withClue applications.toSet.map(_.application.applicationReference.value).mkString(",\n ")

  "findApplicationsAwaitingOverallOutcome" in:

    val applications: Seq[ApplicationWithIndividuals] =
      applicationForRiskingRepo
        .findApplicationsAwaitingOverallOutcome()
        .futureValue

    applications.toSet shouldBe Set(
      TdRiskingInstancesInStates.approved.applicationWithIndividuals,
      TdRiskingInstancesInStates.failedFixable.applicationWithIndividuals,
      TdRiskingInstancesInStates.failedNonFixable.applicationWithIndividuals
    ) withClue applications.toSet.map(_.application.applicationReference.value).mkString(",\n ")

  "findSubscribedReadyForSuccessEmail" in:

    val applications: Seq[ApplicationForRisking] =
      applicationForRiskingRepo
        .findSubscribedReadyForSuccessEmail()
        .futureValue

    applications.toSet shouldBe Set(TdRiskingInstancesInStates.approvedAfterSubscribed.application)

  "findReadyToNotifyBackend returns every application that has a computed riskingOutcome, all risking data (entity + every individual result) is present, and which has not yet been notified to the backend" in:

    val applications: Seq[ApplicationWithIndividuals] =
      applicationForRiskingRepo
        .findReadyToNotifyBackend()
        .futureValue

    applications.toSet shouldBe Set(
      TdRiskingInstancesInStates.approvedAfterOutcome.applicationWithIndividuals,
      TdRiskingInstancesInStates.failedFixableAfterOutcome.applicationWithIndividuals,
      TdRiskingInstancesInStates.failedNonFixableAfterOutcome.applicationWithIndividuals,
      TdRiskingInstancesInStates.partiallyRisked.failedFixable_approved_submitted.applicationWithIndividuals,
      TdRiskingInstancesInStates.partiallyRisked.failedFixable_failedFixable_submitted.applicationWithIndividuals,
      TdRiskingInstancesInStates.partiallyRisked.failedFixable_failedNonFixable_submitted.applicationWithIndividuals,
      TdRiskingInstancesInStates.partiallyRisked.failedFixable_submitted_submitted.applicationWithIndividuals
    ) withClue applications.toSet.map(_.application.applicationReference.value).mkString(",\n ")

  "findAlreadyRiskedApplication" in:

    val riskedApplication = TdRiskingInstancesInStates.failedNonFixableAfterAllEmailsProcessed

    val application: Option[ApplicationWithIndividuals] =
      applicationForRiskingRepo
        .findAlreadyRiskedApplication(riskedApplication.application.applicationReference)
        .futureValue

    application shouldBe Some(riskedApplication.applicationWithIndividuals)

  "setOverallRiskingOutcomeToApprovedForApplication" in:

    val riskedApplication = TdRiskingInstancesInStates.failedNonFixableAfterAllEmailsProcessed.application

    val initialState = applicationForRiskingRepo.findById(riskedApplication.applicationReference).futureValue

    initialState.map(_.overallStatus) shouldBe Some(OverallStatus(
      riskingOutcome = Some(RiskingOutcome.FailedNonFixable),
      emailsProcessed = true,
      backendNotified = true
    ))

    val application: Unit =
      applicationForRiskingRepo
        .setOverallRiskingOutcomeToApprovedForApplication(riskedApplication.applicationReference)
        .futureValue

    val modifiedState = applicationForRiskingRepo.findById(riskedApplication.applicationReference).futureValue

    modifiedState.map(_.overallStatus) shouldBe Some(OverallStatus(
      riskingOutcome = Some(RiskingOutcome.Approved),
      emailsProcessed = false,
      backendNotified = false
    ))

  private val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  private val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  override protected def beforeAll(): Unit =
    super.beforeAll()
    primeDb()
    ()

  private def primeDb(): Unit =
    applicationForRiskingRepo.collection.drop().toFuture.futureValue
    individualForRiskingRepo.collection.drop().toFuture.futureValue
    tdAll
      .tdRiskingInstancesInStates
      .all
      .foreach: td =>
        applicationForRiskingRepo.upsert(td.application).futureValue
        individualForRiskingRepo.upsert(td.individual1).futureValue
        individualForRiskingRepo.upsert(td.individual2).futureValue
