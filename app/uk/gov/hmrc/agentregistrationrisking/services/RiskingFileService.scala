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

import uk.gov.hmrc.agentregistration.shared.risking.ApplicationForRiskingStatus
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileDataRecord

import javax.inject.Inject
import javax.inject.Singleton
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.util.MinervaDateFormats.*
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging

import java.time.Clock
import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class RiskingFileService @Inject() (
  applicationForRiskingRepo: ApplicationForRiskingRepo
)(using
  ExecutionContext,
  Clock
)
extends RequestAwareLogging:

  val clock: Clock = summon[Clock]
  extension (r: RiskingFileDataRecord)
    private def asPipe: String = r.toPipeDelimitedString

  val instant: Instant = Instant.now(clock)
  private val headerRow = s"00|ARR|SAS|${convertToMinervaHeaderDateString(instant)}|${convertToMinervaHeaderTimeString(instant)}"
  private val footerRowPrefix = "99|"

  def buildRiskingFile: Future[(String, Seq[ApplicationForRisking])] =
    for
      applications <- applicationForRiskingRepo.findByStatus(ApplicationForRiskingStatus.ReadyForSubmission)
      totalRecords = applications.map(i => 1 + i.individuals.length).sum
      dataRecordStrings = applications.map(buildDataRecords)
    yield (s"$headerRow\n${dataRecordStrings.mkString("\n")}\n$footerRowPrefix$totalRecords", applications)

  private def buildDataRecords(applicationForRisking: ApplicationForRisking): String =
    val records =
      RiskingFileDataRecord
        .fromApplicationForRisking(applicationForRisking)
        .asPipe
        +: applicationForRisking.individuals.map { i =>
          RiskingFileDataRecord.fromIndividualForRisking(i).asPipe
        }
    records.mkString("\n")
