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

import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes
import org.mongodb.scala.model.Sorts
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import CompletedRiskingRepoHelp.given
import org.mongodb.scala.Document
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.crypto.CompletedRiskingEncryption
import uk.gov.hmrc.agentregistrationrisking.model.CompletedRisking
import uk.gov.hmrc.agentregistrationrisking.model.CompletedRiskingId
import uk.gov.hmrc.agentregistrationrisking.repository.Repo.IdExtractor
import uk.gov.hmrc.agentregistrationrisking.repository.Repo.IdString

@Singleton
final class CompletedRiskingRepo @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig,
  completedRiskingEncryption: CompletedRiskingEncryption
)(using ec: ExecutionContext)
extends Repo[CompletedRiskingId, CompletedRisking](
  collectionName = CompletedRiskingRepo.collectionName,
  mongoComponent = mongoComponent,
  indexes = CompletedRiskingRepoHelp.indexes(appConfig.CompletedRiskingRepo.ttl),
  extraCodecs = Seq(Codecs.playFormatCodec(completedRiskingEncryption.formats)),
  replaceIndexes = true
)(using domainFormat = completedRiskingEncryption.formats):

  def findRecent(applicationReference: ApplicationReference): Future[Option[CompletedRisking]] = collection
    .find(filter = Filters.eq(FieldNames.CompletedRisking.applicationReference, applicationReference.value))
    .sort(Sorts.orderBy(
      Sorts.descending(FieldNames.CompletedRisking.completedAt),
      Sorts.descending(FieldNames.CompletedRisking.completedRiskingId)
    ))
    .limit(1)
    .toFuture()
    .map(_.headOption)

  def findRecent(personReference: PersonReference): Future[Option[CompletedRisking]] = collection
    .find(filter = Filters.eq(FieldNames.CompletedRisking.personReference, personReference.value))
    .sort(Sorts.orderBy(
      Sorts.descending(FieldNames.CompletedRisking.completedAt),
      Sorts.descending(FieldNames.CompletedRisking.completedRiskingId)
    ))
    .limit(1)
    .toFuture()
    .map(_.headOption)

object CompletedRiskingRepo:
  val collectionName = "risking-completed"

object CompletedRiskingRepoHelp:

  given IdString[CompletedRiskingId] =
    new IdString[CompletedRiskingId]:
      override def idString(id: CompletedRiskingId): String = id.value
      override def idField: String = FieldNames.CompletedRisking.completedRiskingId

  given IdExtractor[CompletedRisking, CompletedRiskingId] =
    new IdExtractor[CompletedRisking, CompletedRiskingId]:
      override def id(completedRisking: CompletedRisking): CompletedRiskingId = completedRisking.completedRiskingId

  def indexes(cacheTtl: FiniteDuration): Seq[IndexModel] = Seq(
    IndexModel(
      keys = Indexes.ascending(FieldNames.CompletedRisking.applicationReference),
      indexOptions = IndexOptions()
        .name(FieldNames.CompletedRisking.applicationReferenceIndex)
    ),
    IndexModel(
      keys = Indexes.ascending(FieldNames.CompletedRisking.personReference),
      indexOptions = IndexOptions()
        .name(FieldNames.CompletedRisking.personReferenceIndex)
    ),
    IndexModel(
      keys = Indexes.ascending(FieldNames.CompletedRisking.completedAt),
      indexOptions = IndexOptions()
        .expireAfter(cacheTtl.toSeconds, TimeUnit.SECONDS)
        .name(FieldNames.CompletedRisking.completedAtIndex)
    )
  )
