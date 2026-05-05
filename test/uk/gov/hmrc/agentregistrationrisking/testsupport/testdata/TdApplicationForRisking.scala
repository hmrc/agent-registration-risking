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
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileName

import java.time.Instant
import java.time.temporal.ChronoUnit

object TdApplicationForRisking:

  def make(
    instant: Instant,
    riskingFileName: RiskingFileName,
    applicationReference: ApplicationReference,
    agentApplication: AgentApplication
  ): TdApplicationForRisking =
    new TdApplicationForRisking:
      val instantParam: Instant = instant
      val riskingFileNameParam: RiskingFileName = riskingFileName
      val applicationReferenceParam: ApplicationReference = applicationReference
      val agentApplicationParam: AgentApplication = agentApplication
      override def instant: Instant = instantParam
      override def riskingFileName: RiskingFileName = riskingFileNameParam
      override def applicationReference: ApplicationReference = applicationReferenceParam
      override def agentApplication: AgentApplication = agentApplicationParam

trait TdApplicationForRisking:

  def instant: Instant
  def riskingFileName: RiskingFileName
  def applicationReference: ApplicationReference
  def agentApplication: AgentApplication

  def submitted: ApplicationForRisking = ApplicationForRisking(
    applicationReference = applicationReference,
    riskingFileName = None,
    agentApplication = agentApplication,
    createdAt = instant,
    lastUpdatedAt = instant,
    failures = None,
    isSubscribed = false,
    isEmailSent = false
  )

  def sent: ApplicationForRisking = submitted
    .copy(
      riskingFileName = Some(riskingFileName),
      lastUpdatedAt = instant.plus(1, ChronoUnit.DAYS)
    )

  object receivedRiskingResults:

    val approved: ApplicationForRisking = sent.copy(
      failures = Some(List.empty)
    )

    val failedFixable: ApplicationForRisking = sent.copy(
      failures = Some(List(
        TdFailures.entityFailures.fixable1,
        TdFailures.entityFailures.fixable2
      ))
    )

    val failedNonFixable: ApplicationForRisking = sent.copy(
      failures = Some(List(
        TdFailures.entityFailures.fixable2,
        TdFailures.entityFailures.nonFixable2
      ))
    )
