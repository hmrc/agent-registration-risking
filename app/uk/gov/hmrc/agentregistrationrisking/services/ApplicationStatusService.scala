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
import uk.gov.hmrc.agentregistrationrisking.model.*
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.util.ProcessInSequence
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ApplicationStatusService @Inject() (
  applicationForRiskingRepo: ApplicationForRiskingRepo
)(using
  ExecutionContext
)
extends RequestAwareLogging:

  def processOverallOutcomes()(using RequestHeader): Future[Unit] =
    for
      applicationsWithIndividuals <- applicationForRiskingRepo.findApplicationsAwaitingOverallOutcome()
      eligible: Seq[ApplicationWithIndividuals] = applicationsWithIndividuals.filter(_.individuals.forall(_.individualRiskingResult.isDefined))
      applicationCount: Int = eligible.size
      _ = logger.info(s"Found $applicationCount applications ready to compute overall outcome")
      successCount <-
        ProcessInSequence
          .processAllInSequence(eligible)(computeAndSaveOverallOutcome):
            case (ex, appWithIndividuals) =>
              logger.error(
                s"Failed to compute overall outcome for application ${appWithIndividuals.application.applicationReference.value}",
                ex
              )
      _ = logger.info(s"Computed overall outcome for $successCount/$applicationCount applications")
    yield ()

  private def computeAndSaveOverallOutcome(applicationWithIndividuals: ApplicationWithIndividuals)(using RequestHeader): Future[Unit] =
    RiskingOutcomeHelper.computeRiskingOutcome(applicationWithIndividuals) match
      case None =>
        logger.warn(s"Could not compute overall outcome for ${applicationWithIndividuals.application.applicationReference.value} (missing results)")
        Future.unit
      case Some(outcome) =>
        val updated = applicationWithIndividuals.application
          .modify(_.overallStatus.riskingOutcome)
          .setTo(Some(outcome))
        applicationForRiskingRepo.upsert(updated).map(_ => ())
