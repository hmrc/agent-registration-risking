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
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.risking.RiskingStatus
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRiskingId
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRiskingId
import uk.gov.hmrc.agentregistrationrisking.repository.Repo.IdExtractor
import uk.gov.hmrc.agentregistrationrisking.repository.Repo.IdString

@Singleton
final class IndividualForRiskingRepo @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig
)(using ec: ExecutionContext)
extends Repo[IndividualForRiskingId, IndividualForRisking](
  collectionName = "individual-for-risking",
  mongoComponent = mongoComponent,
  indexes = IndividualForRiskingRepoHelp.indexes(appConfig.ApplicationForRiskingRepo.ttl),
  extraCodecs = Seq(Codecs.playFormatCodec(IndividualForRisking.format)),
  replaceIndexes = true
):

  def findByApplicationForRiskingId(applicationForRiskingId: ApplicationForRiskingId): Future[Seq[IndividualForRisking]] = collection
    .find(
      filter = Filters.eq("applicationForRiskingId", applicationForRiskingId.value)
    )
    .toFuture()

  def findByPersonReference(personReference: PersonReference): Future[Option[IndividualForRisking]] = collection
    .find(
      filter = Filters.eq("individualProvidedDetails.personReference", personReference.value)
    ).headOption()

  def findByStatus(status: RiskingStatus): Future[Seq[IndividualForRisking]] =
    val filter =
      status match
        case RiskingStatus.ReadyForSubmission => Filters.exists("riskingFileId", false)
        case RiskingStatus.SubmittedForRisking => Filters.and(Filters.exists("riskingFileId"), Filters.exists("failures", false))
        case RiskingStatus.ReceivedRiskingResults => Filters.and(Filters.exists("riskingFileId"), Filters.exists("failures"))
    collection.find(filter).toFuture()

object IndividualForRiskingRepoHelp:

  given IdString[IndividualForRiskingId] =
    new IdString[IndividualForRiskingId]:
      override def idString(i: IndividualForRiskingId): String = i.value

  given IdExtractor[IndividualForRisking, IndividualForRiskingId] =
    new IdExtractor[IndividualForRisking, IndividualForRiskingId]:
      override def id(individualForRisking: IndividualForRisking): IndividualForRiskingId = individualForRisking._id

  def indexes(cacheTtl: FiniteDuration): Seq[IndexModel] = Seq(
    IndexModel(
      keys = Indexes.ascending("lastUpdated"),
      indexOptions = IndexOptions().expireAfter(cacheTtl.toSeconds, TimeUnit.SECONDS).name("lastUpdatedIdx")
    ),
    IndexModel(
      keys = Indexes.ascending("applicationForRiskingId"),
      IndexOptions()
        .name("applicationForRiskingIdIdx")
    ),
    IndexModel(
      keys = Indexes.ascending("individualProvidedDetails.personReference"),
      IndexOptions()
        .name("personReferenceIdx")
    ),
    IndexModel(
      keys = Indexes.compoundIndex(Indexes.ascending("individualProvidedDetails.personReference"), Indexes.ascending("applicationForRiskingId")),
      IndexOptions()
        .unique(true)
        .name("personReferenceApplicationIdIdx")
    ),
    IndexModel(
      keys = Indexes.compoundIndex(Indexes.ascending("riskingFileId"), Indexes.ascending("failures")),
      IndexOptions()
        .name("riskingStatusIdx")
    )
  )
