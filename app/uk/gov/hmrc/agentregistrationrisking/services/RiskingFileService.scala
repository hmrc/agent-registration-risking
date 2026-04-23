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

import uk.gov.hmrc.agentregistration.shared.risking.RiskingStatus
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileDataRecord
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.util.MinervaDateFormats.*
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class RiskingFileService @Inject() (
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  individualForRiskingRepo: IndividualForRiskingRepo
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

  def getApplicationsReadyForRiskingWithIndividuals: Future[Seq[ApplicationWithIndividuals]] =
    for
      applications <- applicationForRiskingRepo.findReadyForSubmission()
      applicationsWithIndividuals <-
        Future.traverse(applications): application =>
          individualForRiskingRepo.findByApplicationForRiskingId(application._id)
            .map(individuals => ApplicationWithIndividuals(application, individuals))
    yield applicationsWithIndividuals

  def buildRiskingFileFrom(applicationsWithIndividuals: Seq[ApplicationWithIndividuals]): String =
    val dataRecordString = applicationsWithIndividuals.map(buildDataRecords).mkString("\n")
    val totalRecords = applicationsWithIndividuals.map(a => 1 + a.individuals.length).sum
    s"$headerRow\n$dataRecordString\n$footerRowPrefix$totalRecords\n"

  private def buildDataRecords(appWithIndividuals: ApplicationWithIndividuals): String =
    val records =
      RiskingFileDataRecord
        .fromApplicationForRisking(appWithIndividuals.application)
        .asPipe
        +: appWithIndividuals.individuals.map(i => RiskingFileDataRecord.fromIndividualForRisking(i).asPipe)
    records.mkString("\n")
