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

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.util.Errors.*
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.model.CompletedRisking
import uk.gov.hmrc.agentregistrationrisking.model.CompletedRiskingIdGenerator
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFile
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.CompletedRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.RiskingFileRepo
import uk.gov.hmrc.agentregistrationrisking.util.ProcessInSequence
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class RiskingArchivalService @Inject() (
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  individualForRiskingRepo: IndividualForRiskingRepo,
  completedRiskingRepo: CompletedRiskingRepo,
  riskingFileRepo: RiskingFileRepo,
  completedRiskingIdGenerator: CompletedRiskingIdGenerator,
  clock: Clock
)(using ExecutionContext)
extends RequestAwareLogging:

  def processArchivals()(using RequestHeader): Future[Unit] =
    for
      applicationsWithIndividuals: Seq[ApplicationWithIndividuals] <- applicationForRiskingRepo.findReadyToArchive()
      applicationCount: Int = applicationsWithIndividuals.size
      _ = logger.info(s"Found $applicationCount applications ready to archive")
      archivedCount <-
        ProcessInSequence
          .processAllInSequence(applicationsWithIndividuals)(archive):
            case (ex, applicationWithIndividuals) =>
              logger.error(
                s"Failed to archive application ${applicationWithIndividuals.application.applicationReference.value}",
                ex
              )
      _ = logger.info(s"Archived $archivedCount/$applicationCount applications")
    yield ()

  private def archive(applicationWithIndividuals: ApplicationWithIndividuals)(using RequestHeader): Future[Unit] =
    val application: ApplicationForRisking = applicationWithIndividuals.application
    if applicationWithIndividuals.individuals.isEmpty then
      logger.warn(s"Archiving application ${application.applicationReference.value} with empty individuals")
    for
      riskingFileName <- application.riskingFileName.getOrThrowExpectedDataMissingF("riskingFileName")
      maybeRiskingFile: Option[RiskingFile] <- riskingFileRepo.findById(riskingFileName)
      _ =
        if maybeRiskingFile.isEmpty then
          logger.warn(s"RiskingFile for [${riskingFileName.value}] not found")
      completedRisking: CompletedRisking = CompletedRisking(
        _id = completedRiskingIdGenerator.nextApplicationId(),
        completedAt = Instant.now(clock),
        riskingFile = maybeRiskingFile,
        application = application,
        individuals = applicationWithIndividuals.individuals
      )
      _ <- completedRiskingRepo.upsert(completedRisking)
      _ <- applicationForRiskingRepo.removeById(application.applicationReference)
      _ <- individualForRiskingRepo.deleteByApplicationReference(application.applicationReference)
      _ = logger.info(s"Archived application ${application.applicationReference.value}")
    yield ()
