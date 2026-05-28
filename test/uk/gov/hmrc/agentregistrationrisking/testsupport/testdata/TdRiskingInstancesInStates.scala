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

package uk.gov.hmrc.agentregistrationrisking.testsupport.testdata

import com.softwaremill.quicklens.modify
import uk.gov.hmrc.agentregistration.shared.risking.RiskedEntity
import uk.gov.hmrc.agentregistration.shared.risking.RiskedIndividual
import uk.gov.hmrc.agentregistration.shared.risking.RiskingProgress
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.RiskingOutcome
import uk.gov.hmrc.agentregistrationrisking.testsupport.RichMatchers.*

import java.time.LocalDate
import java.time.temporal.ChronoUnit

trait TdApplicationWithIndividuals:

  def tdRisking: TdRisking
  def application: ApplicationForRisking = tdRisking.tdApplicationForRisking.readyForSubmission
  def individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.readyForSubmission
  def individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.readyForSubmission
  def applicationWithIndividuals: ApplicationWithIndividuals = ApplicationWithIndividuals(
    application = application,
    individuals = Seq(individual1, individual2)
  )
  def riskingProgressForApplicant: RiskingProgress

//TODO
//  def riskingProgressForIndividual1: RiskingProgress
//  def riskingProgressForIndividual2: RiskingProgress

object TdRiskingInstancesInStates:

  val all: Seq[TdApplicationWithIndividuals] = Seq(
    readyForSubmission,
    readyForSubmission2,
    submittedForRisking,
    partiallyRisked.approved_approved_submitted,
    partiallyRisked.approved_failedFixable_submitted,
    partiallyRisked.approved_failedNonFixable_submitted,
    partiallyRisked.approved_submitted_submitted,
    partiallyRisked.failedFixable_approved_submitted,
    partiallyRisked.failedFixable_failedFixable_submitted,
    partiallyRisked.failedFixable_failedNonFixable_submitted,
    partiallyRisked.failedFixable_submitted_submitted,
    partiallyRisked.failedNonFixable_approved_submitted,
    partiallyRisked.failedNonFixable_failedFixable_submitted,
    partiallyRisked.failedNonFixable_failedNonFixable_submitted,
    partiallyRisked.failedNonFixable_submitted_submitted,
    partiallyRisked.submitted_approved_submitted,
    partiallyRisked.submitted_failedFixable_submitted,
    partiallyRisked.submitted_failedNonFixable_submitted,
    approved,
    approvedAfterOutcome,
    approvedAfterSubscribed,
    approvedAfterEmailSent,
    approvedAfterEmailsProcessed,
    failedFixable,
    failedFixableAfterOutcome,
    failedNonFixable,
    failedNonFixableAfterOutcome,
    failedNonFixableAfter1EmailSent,
    failedNonFixableAfter2EmailsSent,
    failedNonFixableAfterAllEmailsSent,
    failedNonFixableAfterAllEmailsProcessed
  )

  case object readyForSubmission
  extends TdApplicationWithIndividuals:

    override val tdRisking: TdRisking = TdRisking.make(this.toString)
    override val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.readyForSubmission
    override val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.readyForSubmission
    override val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.readyForSubmission

    override def riskingProgressForApplicant: RiskingProgress = RiskingProgress.ReadyForSubmission

  case object readyForSubmission2
  extends TdApplicationWithIndividuals:

    override val tdRisking: TdRisking = TdRisking.make(this.toString)
    override val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.readyForSubmission
    override val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.readyForSubmission
    override val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.readyForSubmission

    override def riskingProgressForApplicant: RiskingProgress = RiskingProgress.ReadyForSubmission

  case object submittedForRisking
  extends TdApplicationWithIndividuals:

    override val tdRisking: TdRisking = TdRisking.make(this.toString)
    override val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.submittedForRisking
    override val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.submittedForRisking
    override val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.submittedForRisking

    override def riskingProgressForApplicant: RiskingProgress = RiskingProgress.SubmittedForRisking

  case object partiallyRisked:

    val approved_approved_submitted = partiallyrisked.approved_approved_submitted
    val approved_failedFixable_submitted = partiallyrisked.approved_failedFixable_submitted
    val approved_failedNonFixable_submitted = partiallyrisked.approved_failedNonFixable_submitted
    val approved_submitted_submitted = partiallyrisked.approved_submitted_submitted
    val failedFixable_approved_submitted = partiallyrisked.failedFixable_approved_submitted
    val failedFixable_failedFixable_submitted = partiallyrisked.failedFixable_failedFixable_submitted
    val failedFixable_failedNonFixable_submitted = partiallyrisked.failedFixable_failedNonFixable_submitted
    val failedFixable_submitted_submitted = partiallyrisked.failedFixable_submitted_submitted
    val failedNonFixable_approved_submitted = partiallyrisked.failedNonFixable_approved_submitted
    val failedNonFixable_failedFixable_submitted = partiallyrisked.failedNonFixable_failedFixable_submitted
    val failedNonFixable_failedNonFixable_submitted = partiallyrisked.failedNonFixable_failedNonFixable_submitted
    val failedNonFixable_submitted_submitted = partiallyrisked.failedNonFixable_submitted_submitted
    val submitted_approved_submitted = partiallyrisked.submitted_approved_submitted
    val submitted_failedFixable_submitted = partiallyrisked.submitted_failedFixable_submitted
    val submitted_failedNonFixable_submitted = partiallyrisked.submitted_failedNonFixable_submitted

  case object approved
  extends TdApplicationWithIndividuals:

    override val tdRisking: TdRisking = TdRisking.make(this.toString)
    override val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.receivedRiskingResults.approved
    override val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.approved
    override val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.approved

    override def riskingProgressForApplicant: RiskingProgress = RiskingProgress.Approved

  case object approvedAfterOutcome
  extends TdApplicationWithIndividuals:

    override val tdRisking: TdRisking = TdRisking.make(this.toString)
    override val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.receivedRiskingResults.approvedAfterOutcome
    override val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.approved
    override val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.approved

    override def riskingProgressForApplicant: RiskingProgress = RiskingProgress.Approved

  case object approvedAfterSubscribed
  extends TdApplicationWithIndividuals:

    override val tdRisking: TdRisking = TdRisking.make(this.toString)
    override val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.receivedRiskingResults.approvedAfterSubscribed
    override val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.approved
    override val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.approved

    override def riskingProgressForApplicant: RiskingProgress = RiskingProgress.Approved

  case object approvedAfterEmailSent
  extends TdApplicationWithIndividuals:

    override val tdRisking: TdRisking = TdRisking.make(this.toString)
    override val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.receivedRiskingResults.approvedAfterEmailSent
    override val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.approved
    override val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.approved

    override def riskingProgressForApplicant: RiskingProgress = RiskingProgress.Approved

  case object approvedAfterEmailsProcessed
  extends TdApplicationWithIndividuals:

    override val tdRisking: TdRisking = TdRisking.make(this.toString)
    override val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.receivedRiskingResults.approvedAfterEmailsProcessed
    override val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.approved
    override val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.approved

    override def riskingProgressForApplicant: RiskingProgress = RiskingProgress.Approved

  case object failedFixable
  extends TdApplicationWithIndividuals:

    override val tdRisking: TdRisking = TdRisking.make(this.toString)
    override val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.receivedRiskingResults.approved
    override val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.failedFixable
    override val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.approved

    override val riskingProgressForApplicant: RiskingProgress.FailedFixable = RiskingProgress.FailedFixable(
      riskedEntity = RiskedEntity(
        applicationReference = application.applicationReference,
        failures = Seq.empty
      ),
      riskedIndividuals = Seq(
        RiskedIndividual(
          personReference = individual1.personReference,
          individualName = individual1.individualData.individualName,
          failures = individual1.individualRiskingResult.value.failures
        ),
        RiskedIndividual(
          personReference = individual2.personReference,
          individualName = individual2.individualData.individualName,
          failures = individual2.individualRiskingResult.value.failures
        )
      ),
      riskingCompletedDate = TdInstant.localDate,
      correctiveActionExpiryDate = None
    )

  case object failedFixableAfterOutcome
  extends TdApplicationWithIndividuals:

    override val tdRisking: TdRisking = TdRisking.make(this.toString)
    override val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.receivedRiskingResults.approvedAfterOutcome
      .modify(_.overallStatus.riskingOutcome)
      .setTo(Some(RiskingOutcome.FailedFixable))

    override val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.failedFixable
    override val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.approved

    override val riskingProgressForApplicant: RiskingProgress.FailedFixable = RiskingProgress.FailedFixable(
      riskedEntity = RiskedEntity(
        applicationReference = application.applicationReference,
        failures = Seq.empty
      ),
      riskedIndividuals = Seq(
        RiskedIndividual(
          personReference = individual1.personReference,
          individualName = individual1.individualData.individualName,
          failures = individual1.individualRiskingResult.value.failures
        ),
        RiskedIndividual(
          personReference = individual2.personReference,
          individualName = individual2.individualData.individualName,
          failures = individual2.individualRiskingResult.value.failures
        )
      ),
      riskingCompletedDate = TdInstant.localDate,
      correctiveActionExpiryDate = None
    )

  case object failedNonFixable
  extends TdApplicationWithIndividuals:

    override val tdRisking: TdRisking = TdRisking.make(this.toString)
    override val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.receivedRiskingResults.failedNonFixable
    override val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.failedFixable
    override val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.approved

    override val riskingProgressForApplicant: RiskingProgress.FailedNonFixable = RiskingProgress.FailedNonFixable(
      riskedEntity = RiskedEntity(
        applicationReference = application.applicationReference,
        failures = application.entityRiskingResult.value.failures
      ),
      riskedIndividuals = Seq(
        RiskedIndividual(
          personReference = individual1.personReference,
          individualName = individual1.individualData.individualName,
          failures = individual1.individualRiskingResult.value.failures
        ),
        RiskedIndividual(
          personReference = individual2.personReference,
          individualName = individual2.individualData.individualName,
          failures = individual2.individualRiskingResult.value.failures
        )
      ),
      riskingCompletedDate = TdInstant.localDate,
      correctiveActionExpiryDate = None
    )

  case object failedNonFixableAfterOutcome
  extends TdApplicationWithIndividuals:

    override val tdRisking: TdRisking = TdRisking.make(this.toString)
    override val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.receivedRiskingResults.failedNonFixableAfterOutcome
    override val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.failedFixable
    override val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.approved

    override val riskingProgressForApplicant: RiskingProgress.FailedNonFixable = RiskingProgress.FailedNonFixable(
      riskedEntity = RiskedEntity(
        applicationReference = application.applicationReference,
        failures = application.entityRiskingResult.value.failures
      ),
      riskedIndividuals = Seq(
        RiskedIndividual(
          personReference = individual1.personReference,
          individualName = individual1.individualData.individualName,
          failures = individual1.individualRiskingResult.value.failures
        ),
        RiskedIndividual(
          personReference = individual2.personReference,
          individualName = individual2.individualData.individualName,
          failures = individual2.individualRiskingResult.value.failures
        )
      ),
      riskingCompletedDate = TdInstant.localDate,
      correctiveActionExpiryDate = Some(TdInstant.correctiveActionExpiryLocalDate)
    )

  case object failedNonFixableAfter1EmailSent
  extends TdApplicationWithIndividuals:

    override val tdRisking: TdRisking = TdRisking.make(this.toString)
    override val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.receivedRiskingResults.failedNonFixableAfterEmailSent
    override val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.failedNonFixable
    override val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.failedNonFixable

    override val riskingProgressForApplicant: RiskingProgress.FailedNonFixable = RiskingProgress.FailedNonFixable(
      riskedEntity = RiskedEntity(
        applicationReference = application.applicationReference,
        failures = application.entityRiskingResult.value.failures
      ),
      riskedIndividuals = Seq(
        RiskedIndividual(
          personReference = individual1.personReference,
          individualName = individual1.individualData.individualName,
          failures = individual1.individualRiskingResult.value.failures
        ),
        RiskedIndividual(
          personReference = individual2.personReference,
          individualName = individual2.individualData.individualName,
          failures = individual2.individualRiskingResult.value.failures
        )
      ),
      riskingCompletedDate = TdInstant.localDate,
      correctiveActionExpiryDate = Some(TdInstant.correctiveActionExpiryLocalDate)
    )

  case object failedNonFixableAfter2EmailsSent
  extends TdApplicationWithIndividuals:

    override val tdRisking: TdRisking = TdRisking.make(this.toString)
    override val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.receivedRiskingResults.failedNonFixableAfterEmailSent
    override val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.failedNonFixableEmailSent
    override val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.failedNonFixable

    override val riskingProgressForApplicant: RiskingProgress.FailedNonFixable = RiskingProgress.FailedNonFixable(
      riskedEntity = RiskedEntity(
        applicationReference = application.applicationReference,
        failures = application.entityRiskingResult.value.failures
      ),
      riskedIndividuals = Seq(
        RiskedIndividual(
          personReference = individual1.personReference,
          individualName = individual1.individualData.individualName,
          failures = individual1.individualRiskingResult.value.failures
        ),
        RiskedIndividual(
          personReference = individual2.personReference,
          individualName = individual2.individualData.individualName,
          failures = individual2.individualRiskingResult.value.failures
        )
      ),
      riskingCompletedDate = TdInstant.localDate,
      correctiveActionExpiryDate = Some(TdInstant.correctiveActionExpiryLocalDate)
    )

  case object failedNonFixableAfterAllEmailsSent
  extends TdApplicationWithIndividuals:

    override val tdRisking: TdRisking = TdRisking.make(this.toString)
    override val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.receivedRiskingResults.failedNonFixableAfterEmailSent
    override val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.failedNonFixableEmailSent
    override val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.failedNonFixableEmailSent

    override val riskingProgressForApplicant: RiskingProgress.FailedNonFixable = RiskingProgress.FailedNonFixable(
      riskedEntity = RiskedEntity(
        applicationReference = application.applicationReference,
        failures = application.entityRiskingResult.value.failures
      ),
      riskedIndividuals = Seq(
        RiskedIndividual(
          personReference = individual1.personReference,
          individualName = individual1.individualData.individualName,
          failures = individual1.individualRiskingResult.value.failures
        ),
        RiskedIndividual(
          personReference = individual2.personReference,
          individualName = individual2.individualData.individualName,
          failures = individual2.individualRiskingResult.value.failures
        )
      ),
      riskingCompletedDate = TdInstant.localDate,
      correctiveActionExpiryDate = Some(TdInstant.correctiveActionExpiryLocalDate)
    )

  case object failedNonFixableAfterAllEmailsProcessed
  extends TdApplicationWithIndividuals:

    override val tdRisking: TdRisking = TdRisking.make(this.toString)
    override val application: ApplicationForRisking =
      tdRisking
        .tdApplicationForRisking
        .receivedRiskingResults
        .failedNonFixableAfterEmailsProcessed

    override val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.failedNonFixableEmailSent
    override val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.failedNonFixableEmailSent

    override val riskingProgressForApplicant: RiskingProgress.FailedNonFixable = RiskingProgress.FailedNonFixable(
      riskedEntity = RiskedEntity(
        applicationReference = application.applicationReference,
        failures = application.entityRiskingResult.value.failures
      ),
      riskedIndividuals = Seq(
        RiskedIndividual(
          personReference = individual1.personReference,
          individualName = individual1.individualData.individualName,
          failures = individual1.individualRiskingResult.value.failures
        ),
        RiskedIndividual(
          personReference = individual2.personReference,
          individualName = individual2.individualData.individualName,
          failures = individual2.individualRiskingResult.value.failures
        )
      ),
      riskingCompletedDate = TdInstant.localDate,
      correctiveActionExpiryDate = Some(TdInstant.correctiveActionExpiryLocalDate)
    )
