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

import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.EntityRiskingResult
import uk.gov.hmrc.agentregistrationrisking.model.OverallStatus
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileName
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcome

import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import com.softwaremill.quicklens.modify
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicationData

object TdApplicationForRisking:

  def make(
    instant: Instant,
    riskingFileName: RiskingFileName,
    applicationData: ApplicationData
  ): TdApplicationForRisking =

    val instantParam: Instant = instant
    val riskingFileNameParam: RiskingFileName = riskingFileName
    val applicationDataParam: ApplicationData = applicationData

    new TdApplicationForRisking:
      override def instant: Instant = instantParam
      override def riskingFileName: RiskingFileName = riskingFileNameParam
      override def applicationReference: ApplicationReference = applicationDataParam.applicationReference
      override def applicationData: ApplicationData = applicationDataParam

trait TdApplicationForRisking:

  def instant: Instant
  def riskingFileName: RiskingFileName
  def applicationReference: ApplicationReference
  def applicationData: ApplicationData

  def correctiveActionExpiryDate: Instant = instant.plus(Duration.ofDays(45))

  def readyForSubmission: ApplicationForRisking = ApplicationForRisking(
    applicationReference = applicationReference,
    riskingFileName = None,
    applicationData = applicationData,
    createdAt = instant,
    lastUpdatedAt = instant,
    entityRiskingResult = None,
    isSubscribed = false,
    isEmailSent = false,
    overallStatus = OverallStatus(
      riskingOutcome = None,
      emailsProcessed = false,
      backendNotified = Some(false)
    ),
    correctiveActionExpiryDate = None
  )

  def submittedForRisking: ApplicationForRisking = readyForSubmission
    .copy(
      riskingFileName = Some(riskingFileName),
      lastUpdatedAt = instant
    )

  object receivedRiskingResults:

    val approved: ApplicationForRisking = submittedForRisking.copy(
      entityRiskingResult = Some(EntityRiskingResult(
        failures = List.empty,
        receivedAt = instant
      ))
    )

    val approvedAfterOutcome: ApplicationForRisking = approved
      .modify(_.overallStatus.riskingOutcome)
      .setTo(Some(RiskingOutcome.Approved))

    val approvedAfterSubscribed: ApplicationForRisking = approvedAfterOutcome.copy(isSubscribed = true)

    val approvedAfterEmailSent: ApplicationForRisking = approvedAfterSubscribed
      .copy(isEmailSent = true)

    val approvedAfterEmailsProcessed: ApplicationForRisking = approvedAfterEmailSent
      .modify(_.overallStatus.emailsProcessed).setTo(true)

    val failedFixable: ApplicationForRisking = submittedForRisking.copy(
      entityRiskingResult = Some(EntityRiskingResult(
        failures = List(
          TdFailures.entityFailures.fixable1,
          TdFailures.entityFailures.fixable2
        ),
        receivedAt = instant
      ))
    )

    val failedFixableAfterOutcome: ApplicationForRisking = failedFixable
      .modify(_.overallStatus.riskingOutcome)
      .setTo(Some(RiskingOutcome.FailedFixable))
      .modify(_.correctiveActionExpiryDate)
      .setTo(Some(correctiveActionExpiryDate))

    val failedNonFixable: ApplicationForRisking = submittedForRisking
      .copy(
        entityRiskingResult = Some(EntityRiskingResult(
          failures = List(
            TdFailures.entityFailures.fixable2,
            TdFailures.entityFailures.nonFixable2
          ),
          receivedAt = instant
        ))
      )

    val failedNonFixableAfterOutcome: ApplicationForRisking = failedNonFixable
      .modify(_.overallStatus.riskingOutcome)
      .setTo(Some(RiskingOutcome.FailedNonFixable))
      .modify(_.correctiveActionExpiryDate)
      .setTo(Some(correctiveActionExpiryDate))

    val failedNonFixableAfterEmailSent: ApplicationForRisking = failedNonFixableAfterOutcome.copy(
      isEmailSent = true
    )

    val failedNonFixableAfterEmailsProcessed: ApplicationForRisking = failedNonFixableAfterEmailSent
      .modify(_.overallStatus.emailsProcessed)
      .setTo(true)
