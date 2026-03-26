/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationrisking.repository

import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import ResultsFileLogRepoHelp.given
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationForRiskingStatus
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.risking.PersonReference
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.RiskingResultsFile
import uk.gov.hmrc.agentregistrationrisking.model.ResultsFileName
import uk.gov.hmrc.agentregistrationrisking.model.ResultsFileProcessingStatus
import uk.gov.hmrc.agentregistrationrisking.repository.Repo.IdExtractor
import uk.gov.hmrc.agentregistrationrisking.repository.Repo.IdString

@Singleton
final class ResultsFileLogRepo @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig
)(using ec: ExecutionContext)
extends Repo[ResultsFileName, RiskingResultsFile](
  collectionName = "results-file-log",
  mongoComponent = mongoComponent,
  indexes = ResultsFileLogRepoHelp.indexes(appConfig.ResultsFileLogRepo.ttl),
  extraCodecs = Seq(Codecs.playFormatCodec(RiskingResultsFile.format)),
  replaceIndexes = true
):

  def findAll(): Future[Seq[RiskingResultsFile]] = collection.find().toFuture()

  def findByFileName(fileName: ResultsFileName): Future[Option[RiskingResultsFile]] = collection
    .find(
      filter = Filters.eq("fileName", fileName.value)
    )
    .headOption()

  def findByStatus(status: ResultsFileProcessingStatus): Future[Seq[RiskingResultsFile]] = collection
    .find(
      filter = Filters.eq("status", status.toString)
    ).toFuture()

// when named ResultsFileLogRepo, Scala 3 compiler complains
// about cyclic reference error during compilation ...
object ResultsFileLogRepoHelp:

  given IdString[ResultsFileName] =
    new IdString[ResultsFileName]:
      override def idString(i: ResultsFileName): String = i.value

  given IdExtractor[RiskingResultsFile, ResultsFileName] =
    new IdExtractor[RiskingResultsFile, ResultsFileName]:
      override def id(resultsFileLog: RiskingResultsFile): ResultsFileName = resultsFileLog.fileName

  def indexes(cacheTtl: FiniteDuration): Seq[IndexModel] = Seq(
    IndexModel(
      keys = Indexes.ascending("fileName"),
      IndexOptions()
        .unique(true)
        .name("fileName")
    ),
    IndexModel(
      keys = Indexes.ascending("status"),
      IndexOptions()
        .unique(false)
        .name("status")
    )
  )
