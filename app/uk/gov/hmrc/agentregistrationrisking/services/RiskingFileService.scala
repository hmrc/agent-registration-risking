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

import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFile
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileDataRecord
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileName
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileWithContent
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileWithContent.*
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.util.MinervaDateFormats.*
import uk.gov.hmrc.agentregistrationrisking.util.MinervaDateFormats
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging

import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object RiskingFileService:

  def buildRiskingFileWithContent(
    applications: Seq[ApplicationForRisking],
    individuals: Seq[IndividualForRisking],
    instant: Instant
  ): RiskingFileWithContent =
    // TODO: when resubmitting, THE APPROVED should be REMOVED from the below lists so they won't be sent for risking twice
    val riskingFile: RiskingFile = RiskingFile(
      riskingFileName = RiskingFileName.make(instant),
      uploadedAt = instant
    )
    val (
      riskingFileContent: RiskingFileContent,
      numberOfRecords: NumberOfRecords
    ) = buildRiskingFileContent(
      applications,
      individuals,
      instant
    )
    RiskingFileWithContent(
      riskingFile = riskingFile,
      riskingFileContent = riskingFileContent,
      numberOfRecords = numberOfRecords
    )

  private def buildRiskingFileContent(
    applications: Seq[ApplicationForRisking],
    individuals: Seq[IndividualForRisking],
    instant: Instant
  ): (RiskingFileContent, NumberOfRecords) =
    val headerRow: String = s"00|ARR|SAS|${asMinervaHeaderDate(instant)}|${asMinervaHeaderTime(instant)}\n"
    val (dataRecords: String, footerRecord: String, numberOfRecords: NumberOfRecords) =
      val applicationRecords: Seq[RiskingFileDataRecord] = applications.map(RiskingFileDataRecord.fromApplicationForRisking)
      val individualRecords: Seq[RiskingFileDataRecord] = individuals.map(RiskingFileDataRecord.fromIndividualForRisking)
      val allRecords: Seq[RiskingFileDataRecord] = applicationRecords ++ individualRecords
      val numberOfRecords: NumberOfRecords = allRecords.size
      val footerRow = s"99|$numberOfRecords\n"
      (
        allRecords
          .map(_.toPipeDelimitedString)
          .mkString("", "\n", "\n"),
        footerRow,
        numberOfRecords
      )

    val riskingFileContent: RiskingFileContent = headerRow + dataRecords + footerRecord
    (riskingFileContent, numberOfRecords)
