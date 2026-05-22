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
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistrationrisking.audit.AuditService
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.model.*
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.util.ProcessInSequence
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging

import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ApplicationOutcomeService @Inject() (
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  auditService: AuditService,
  appConfig: AppConfig,
  clock: Clock
)(using
  ExecutionContext
)
extends RequestAwareLogging:

  def processOverallOutcomes()(using RequestHeader): Future[Unit] =
    for
      applicationWithIndividuals: Seq[ApplicationWithIndividuals] <- applicationForRiskingRepo.findReadyToSetRiskingOutcome()
      applicationCount: Int = applicationWithIndividuals.size
      _ = logger.info(s"Found $applicationCount applications ready to compute overall outcome")
      updatedOutcomeCount <-
        ProcessInSequence
          .processAllInSequence(applicationWithIndividuals)(computeAndSaveOverallOutcome):
            case (ex, appWithIndividuals) =>
              logger.error(
                s"Failed to compute overall outcome for application ${appWithIndividuals.application.applicationReference.value}",
                ex
              )
      _ = logger.info(s"Successfully computed overall outcome for $updatedOutcomeCount/$applicationCount applications")
    yield ()

  private def computeAndSaveOverallOutcome(applicationWithIndividuals: ApplicationWithIndividuals)(using RequestHeader): Future[Unit] =
    RiskingOutcomeHelper.computeRiskingOutcome(applicationWithIndividuals) match
      case None =>
        logger.error(s"BUG: Missing risking results for application ${applicationWithIndividuals.application.applicationReference} - this should not happen")
        Future.unit
      case Some(outcome) =>
        val applicationForRisking: ApplicationForRisking = applicationWithIndividuals
          .application
          .modify(_.overallStatus.riskingOutcome)
          .setTo(Some(outcome))
          .modify(_.failureMessageExpiryDate)
          .setTo(failureMessageExpiryDateFor(outcome))
        applicationForRiskingRepo
          .upsert(applicationForRisking)
          .map: _ =>
            auditService.sendRiskingDeterminationEvent(applicationForRisking.applicationReference, outcome)

  private def failureMessageExpiryDateFor(outcome: RiskingOutcome): Option[Instant] =
    outcome match
      case RiskingOutcome.FailedFixable | RiskingOutcome.FailedNonFixable =>
        Some(Instant.now(clock).plus(Duration.ofDays(appConfig.FailureMessage.daysToDisplayErrorMessage.toLong)))
      case RiskingOutcome.Approved => None
