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

package uk.gov.hmrc.agentregistrationrisking.runner

import play.api.mvc.Headers
import play.api.mvc.RequestHeader
import play.api.mvc.request.RemoteConnection
import play.api.mvc.request.RequestTarget
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFile
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileName
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileWithContent
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.RiskingFileRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.services.ObjectStoreService
import uk.gov.hmrc.agentregistrationrisking.services.RiskingFileService
import uk.gov.hmrc.agentregistrationrisking.services.SdesProxyService
import uk.gov.hmrc.objectstore.client.ObjectSummaryWithMd5
import play.api.libs.typedmap.TypedMap
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistrationrisking.util.EmptyRequest
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class RiskingRunner @Inject() (
  objectStoreService: ObjectStoreService,
  sdesProxyService: SdesProxyService,
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  individualForRiskingRepo: IndividualForRiskingRepo,
  riskingFileRepo: RiskingFileRepo
)(using
  ec: ExecutionContext,
  clock: Clock
)
extends RequestAwareLogging:

  def run(): Future[Unit] =
    given RequestHeader = EmptyRequest.emptyRequestHeader
    logger.info(s"Building risking file and sending it to minerva started ...")
    val instant: Instant = Instant.now(clock)
    for
      applications: Seq[ApplicationForRisking] <- applicationForRiskingRepo.findReadyForSubmission()
      _ = logger.info(s"Found ${applications.size} applications ready for submission")
      applicationReferences: Seq[ApplicationReference] = applications.map(_.applicationReference)
      individuals: Seq[IndividualForRisking] <- individualForRiskingRepo.findByApplicationReferences(applicationReferences)
      _ = logger.info(s"Found ${individuals.size} corresponding individuals")
      riskingFileWithContent: RiskingFileWithContent = RiskingFileService.buildRiskingFileWithContent(
        applications = applications,
        individuals = individuals,
        instant = instant
      )
      riskingFileName: RiskingFileName = riskingFileWithContent.riskingFile.riskingFileName
      _ = logger.info(s"Generated risking file: $riskingFileName, ${riskingFileWithContent.numberOfRecords} records")
      objectSummary: ObjectSummaryWithMd5 <- objectStoreService.uploadRiskingFile(riskingFileWithContent)
      _ = logger.info(s"Uploaded risking file to object store: ${objectSummary.location}")
      _ <- riskingFileRepo.upsert(riskingFileWithContent.riskingFile)
      _ = logger.info(s"Persisted risking file: ${riskingFileWithContent.riskingFile}")
      _ <- applicationForRiskingRepo.updateRiskingFileId(
        applicationReferences = applicationReferences,
        riskingFileName = riskingFileName
      )
      _ = logger.info(s"Updated applications as submitted for risking in $riskingFileName")
      _ <- sdesProxyService.notifySdesFileReady(objectSummary)
      _ = logger.info(s"Sent notification to SDES")
      _ = logger.info(
        s"""Risking file built and sent to minerva successfully:
           | ${riskingFileWithContent.riskingFile}
           | ${riskingFileWithContent.numberOfRecords} records
           | $objectSummary
           |""".stripMargin
      )
    yield ()
