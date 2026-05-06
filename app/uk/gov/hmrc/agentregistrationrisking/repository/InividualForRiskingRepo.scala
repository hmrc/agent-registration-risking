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
import IndividualForRiskingRepoHelp.given
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.repository.Repo.IdExtractor
import uk.gov.hmrc.agentregistrationrisking.repository.Repo.IdString

@Singleton
final class IndividualForRiskingRepo @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig
)(using ec: ExecutionContext)
extends Repo[PersonReference, IndividualForRisking](
  collectionName = "individual-for-risking",
  mongoComponent = mongoComponent,
  indexes = IndividualForRiskingRepoHelp.indexes(appConfig.ApplicationForRiskingRepo.ttl),
  extraCodecs = Seq(Codecs.playFormatCodec(IndividualForRisking.format)),
  replaceIndexes = true
):

  def insertMany(individualForRiskingList: List[IndividualForRisking]): Future[Unit] = collection.insertMany(individualForRiskingList).toFuture().map(_ => ())

  def findByApplicationReference(applicationReference: ApplicationReference): Future[Seq[IndividualForRisking]] = collection
    .find(
      filter = Filters.eq(FieldNames.applicationReference, applicationReference.value)
    )
    .toFuture()

  def findByApplicationReferences(applicationReferences: Seq[ApplicationReference]): Future[Seq[IndividualForRisking]] = collection
    .find(
      filter = Filters.in(FieldNames.applicationReference, applicationReferences.map(_.value))
    )
    .toFuture()

object IndividualForRiskingRepoHelp:

  given IdString[PersonReference] =
    new IdString[PersonReference]:
      override def idString(id: PersonReference): String = id.value
      override def idField: String = FieldNames.personReference

  given IdExtractor[IndividualForRisking, PersonReference] =
    new IdExtractor[IndividualForRisking, PersonReference]:
      override def id(individualForRisking: IndividualForRisking): PersonReference = individualForRisking.personReference

  def indexes(ttl: FiniteDuration): Seq[IndexModel] = Seq(
    IndexModel(
      keys = Indexes.ascending(FieldNames.personReference),
      indexOptions = IndexOptions()
        .name(FieldNames.personReferenceIndex)
        .unique(true)
    ),
    IndexModel(
      keys = Indexes.ascending(FieldNames.applicationReference),
      indexOptions = IndexOptions()
        .name(FieldNames.applicationReferenceIndex)
        .unique(false)
    ),
    IndexModel(
      keys = Indexes.ascending(FieldNames.lastUpdatedAt),
      indexOptions = IndexOptions()
        .expireAfter(ttl.toSeconds, TimeUnit.SECONDS)
        .name(FieldNames.lastUpdatedAtIndex)
    )
  )
