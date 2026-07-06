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
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailures
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcome
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeRequest
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistrationrisking.connectors.AgentRegistrationConnector
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.model.EntityRiskingResult
import uk.gov.hmrc.agentregistrationrisking.model.IndividualRiskingResult
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.services.RiskingOutcomeHelper.outcome
import uk.gov.hmrc.agentregistrationrisking.services.RiskingOutcomeHelper.outcomeForEntity
import uk.gov.hmrc.agentregistrationrisking.util.ProcessInSequence
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class BackendNotificationService @Inject() (
  agentRegistrationConnector: AgentRegistrationConnector,
  applicationForRiskingRepo: ApplicationForRiskingRepo
)(using ExecutionContext)
extends RequestAwareLogging:

  def processBackendNotifications()(using RequestHeader): Future[Unit] =
    for
      applicationsWithIndividuals: Seq[ApplicationWithIndividuals] <- applicationForRiskingRepo.findReadyToNotifyBackend()
      applicationCount: Int = applicationsWithIndividuals.size
      _ = logger.info(s"Found $applicationCount applications ready to notify backend")
      notifiedCount <-
        ProcessInSequence
          .processAllInSequence(applicationsWithIndividuals)(process):
            case (ex, applicationWithIndividuals) =>
              logger.error(s"Failed to notify backend for application ${applicationWithIndividuals.application.applicationReference.value}", ex)
      _ = logger.info(s"Notified backend for $notifiedCount/$applicationCount applications")
    yield ()

  private def process(applicationWithIndividuals: ApplicationWithIndividuals)(using RequestHeader): Future[Unit] =
    val applicationForRisking: ApplicationForRisking = applicationWithIndividuals.application
    val riskingOutcomeRequest: RiskingOutcomeRequest = buildRiskingOutcomeRequest(applicationWithIndividuals)
    for
      _ <- agentRegistrationConnector.sendRiskingOutcome(applicationForRisking.applicationReference, riskingOutcomeRequest)
      _ <- applicationForRiskingRepo.upsert(applicationForRisking.modify(_.overallStatus.backendNotified).setTo(true))
      _ = logger.info(s"Notified backend for applicationForRisking ${applicationForRisking.applicationReference}")
    yield ()

  private def buildRiskingOutcomeRequest(applicationWithIndividuals: ApplicationWithIndividuals): RiskingOutcomeRequest =
    val applicationForRisking: ApplicationForRisking = applicationWithIndividuals.application
    val entityRiskingResult: EntityRiskingResult = applicationForRisking.entityRiskingResult.getOrThrowExpectedDataMissing("entityRiskingResult")
    val individualFailuresList: Seq[IndividualFailures] = applicationWithIndividuals.individuals.map: individual =>
      val individualRiskingResult: IndividualRiskingResult = individual.individualRiskingResult.getOrThrowExpectedDataMissing(
        s"individualRiskingResult for personReference [${individual.individualData.personReference.value}]"
      )
      IndividualFailures(
        personReference = individual.individualData.personReference,
        failures = individualRiskingResult.failures,
        riskingOutcome = individualRiskingResult.failures.outcome
      )
    val emailSentAt: Instant = applicationForRisking.overallStatus.emailSentAt.getOrThrowExpectedDataMissing("overallStatus.emailSentAt")
    val riskingOutcome: RiskingOutcome = applicationForRisking.overallStatus.riskingOutcome.getOrThrowExpectedDataMissing("riskingOutcome")
    val riskingCompletedDate: LocalDate = emailSentAt.atZone(ZoneOffset.UTC).toLocalDate
    RiskingOutcomeRequest(
      riskingCompletedDate = riskingCompletedDate,
      applicationOutcome = riskingOutcome,
      entityFailures = entityRiskingResult.failures,
      entityOutcome = entityRiskingResult.failures.outcomeForEntity,
      individualFailures = individualFailuresList
    )
