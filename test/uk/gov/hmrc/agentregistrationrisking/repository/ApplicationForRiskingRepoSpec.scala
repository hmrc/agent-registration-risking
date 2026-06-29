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
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
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

    applications.size shouldBe 1 withClue applications.map(_.applicationReference.value).mkString(", ")
    applications.toSet shouldBe Set(TdRiskingInstancesInStates.approvedAfterOutcome.application)

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

  "findReadyToNotifyBackend returns every application whose outcome-specific upstream work is complete and which has not yet been notified to the backend" in:

    val applications: Seq[ApplicationWithIndividuals] =
      applicationForRiskingRepo
        .findReadyToNotifyBackend()
        .futureValue

    applications.toSet shouldBe Set(
      TdRiskingInstancesInStates.approvedAfterEmailSent.applicationWithIndividuals,
      TdRiskingInstancesInStates.approvedAfterEmailsProcessed.applicationWithIndividuals,
      TdRiskingInstancesInStates.failedFixableAfterOutcome.applicationWithIndividuals,
      TdRiskingInstancesInStates.failedNonFixableAfterAllEmailsProcessed.applicationWithIndividuals,
      TdRiskingInstancesInStates.partiallyRisked.failedFixable_approved_submitted.applicationWithIndividuals,
      TdRiskingInstancesInStates.partiallyRisked.failedFixable_failedFixable_submitted.applicationWithIndividuals,
      TdRiskingInstancesInStates.partiallyRisked.failedFixable_failedNonFixable_submitted.applicationWithIndividuals,
      TdRiskingInstancesInStates.partiallyRisked.failedFixable_submitted_submitted.applicationWithIndividuals
    ) withClue applications.toSet.map(_.application.applicationReference.value).mkString(",\n ")

  "findReadyToNotifyBackend excludes Approved applications whose subscription has not completed yet" in:
    val applications: Set[ApplicationWithIndividuals] = applicationForRiskingRepo.findReadyToNotifyBackend().futureValue.toSet
    applications should not contain TdRiskingInstancesInStates.approvedAfterOutcome.applicationWithIndividuals

  "findReadyToNotifyBackend excludes Approved applications that are subscribed but whose success email has not been sent" in:
    val applications: Set[ApplicationWithIndividuals] = applicationForRiskingRepo.findReadyToNotifyBackend().futureValue.toSet
    applications should not contain TdRiskingInstancesInStates.approvedAfterSubscribed.applicationWithIndividuals

  "findReadyToNotifyBackend excludes FailedNonFixable applications whose emails are not yet fully processed" in:
    val applications: Set[ApplicationWithIndividuals] = applicationForRiskingRepo.findReadyToNotifyBackend().futureValue.toSet
    applications should not contain TdRiskingInstancesInStates.failedNonFixableAfterOutcome.applicationWithIndividuals
    applications should not contain TdRiskingInstancesInStates.failedNonFixableAfter1EmailSent.applicationWithIndividuals
    applications should not contain TdRiskingInstancesInStates.failedNonFixableAfter2EmailsSent.applicationWithIndividuals
    applications should not contain TdRiskingInstancesInStates.failedNonFixableAfterAllEmailsSent.applicationWithIndividuals

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
