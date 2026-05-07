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

package uk.gov.hmrc.agentregistrationrisking.testOnly.repos

import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.repository.FieldNames
import uk.gov.hmrc.agentregistrationrisking.repository.Repo
import uk.gov.hmrc.agentregistrationrisking.repository.Repo.IdExtractor
import uk.gov.hmrc.agentregistrationrisking.repository.Repo.IdString
import uk.gov.hmrc.agentregistrationrisking.testOnly.model.RiskingResultsFileContent
import uk.gov.hmrc.agentregistrationrisking.testOnly.model.RiskingResultsFileName
import uk.gov.hmrc.agentregistrationrisking.testOnly.repos.RiskingResultFilesRepoHelp.given
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

@Singleton
final class RiskingResultsFileContentsRepo @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig
)(using ec: ExecutionContext)
extends Repo[RiskingResultsFileName, RiskingResultsFileContent](
  collectionName = "risking-results-file-contents",
  mongoComponent = mongoComponent,
  indexes = RiskingResultFilesRepoHelp.indexes(appConfig.ApplicationForRiskingRepo.ttl),
  extraCodecs = Seq(Codecs.playFormatCodec(RiskingResultsFileContent.format)),
  replaceIndexes = true
):

  def findAll(): Future[Seq[RiskingResultsFileContent]] = collection.find[RiskingResultsFileContent]().toFuture()

object RiskingResultFilesRepoHelp:

  object FieldNames:

    val riskingResultsFileName: String = "riskingResultsFileName"
    val riskingResultsFileNameIndex: String = "riskingResultsFileNameIndex"
    val uploadedAt: String = "uploadedAt"
    val uploadedAtIndex: String = "uploadedAtIndex"

  given IdString[RiskingResultsFileName] =
    new IdString[RiskingResultsFileName]:
      override def idString(id: RiskingResultsFileName): String = id.value
      override def idField: String = FieldNames.riskingResultsFileName

  given IdExtractor[RiskingResultsFileContent, RiskingResultsFileName] =
    new IdExtractor[RiskingResultsFileContent, RiskingResultsFileName]:
      override def id(riskingResultsFileContent: RiskingResultsFileContent): RiskingResultsFileName = riskingResultsFileContent.riskingResultsFileName

  def indexes(cacheTtl: FiniteDuration): Seq[IndexModel] = Seq(
    IndexModel(
      keys = Indexes.ascending(FieldNames.riskingResultsFileName),
      indexOptions = IndexOptions()
        .name(FieldNames.riskingResultsFileNameIndex)
        .unique(true)
    ),
    IndexModel(
      keys = Indexes.ascending(FieldNames.uploadedAt),
      indexOptions = IndexOptions()
        .expireAfter(RiskingResultsFileContent.ttl.toSeconds, TimeUnit.SECONDS)
        .name(FieldNames.uploadedAtIndex)
    )
  )
