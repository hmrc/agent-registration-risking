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

package uk.gov.hmrc.agentregistrationrisking.repository

import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import RiskingFileRepoHelp.given
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFile
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileName
import uk.gov.hmrc.agentregistrationrisking.repository.Repo.IdExtractor
import uk.gov.hmrc.agentregistrationrisking.repository.Repo.IdString

@Singleton
final class RiskingFileRepo @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig
)(using ec: ExecutionContext)
extends Repo[RiskingFileName, RiskingFile](
  collectionName = "risking-file",
  mongoComponent = mongoComponent,
  indexes = RiskingFileRepoHelp.indexes(appConfig.ApplicationForRiskingRepo.ttl),
  extraCodecs = Seq(Codecs.playFormatCodec(RiskingFile.format)),
  replaceIndexes = true
)

object RiskingFileRepoHelp:

  given IdString[RiskingFileName] =
    new IdString[RiskingFileName]:
      override def idString(id: RiskingFileName): String = id.value
      override def idField: String = FieldNames.riskingFileName

  given IdExtractor[RiskingFile, RiskingFileName] =
    new IdExtractor[RiskingFile, RiskingFileName]:
      override def id(riskingFile: RiskingFile): RiskingFileName = riskingFile.riskingFileName

  def indexes(cacheTtl: FiniteDuration): Seq[IndexModel] = Seq(
    IndexModel(
      keys = Indexes.ascending(FieldNames.riskingFileName),
      indexOptions = IndexOptions()
        .name(FieldNames.riskingFileNameIndex)
        .unique(true)
    ),
    IndexModel(
      keys = Indexes.ascending(FieldNames.uploadedAt),
      indexOptions = IndexOptions()
        .expireAfter(cacheTtl.toSeconds, TimeUnit.SECONDS)
        .name(FieldNames.uploadedAtIndex)
    )
  )
