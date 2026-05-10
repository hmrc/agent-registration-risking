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

import org.bson.json.JsonMode
import org.bson.json.JsonWriterSettings
import org.mongodb.scala.Document
import org.mongodb.scala.model.Aggregates
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes
import org.mongodb.scala.model.Updates
import play.api.libs.json.Json
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Aggregates
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes
import org.mongodb.scala.model.Updates
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import ApplicationForRiskingRepoHelp.given
import org.mongodb.scala.result.UpdateResult
import com.mongodb.client.model.Field
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileName
import uk.gov.hmrc.agentregistrationrisking.repository.Repo.IdExtractor
import uk.gov.hmrc.agentregistrationrisking.repository.Repo.IdString

import java.time.Clock
import java.time.Instant

@Singleton
final class ApplicationForRiskingRepo @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig,
  clock: Clock
)(using ec: ExecutionContext)
extends Repo[ApplicationReference, ApplicationForRisking](
  collectionName = "application-for-risking",
  mongoComponent = mongoComponent,
  indexes = ApplicationForRiskingRepoHelp.indexes(appConfig.ApplicationForRiskingRepo.ttl),
  extraCodecs = Seq(Codecs.playFormatCodec(ApplicationForRisking.format)),
  replaceIndexes = true
):

  private val relaxedJson = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build()

  def getApplicationWithIndividuals(applicationReference: ApplicationReference): Future[
    Option[ApplicationWithIndividuals]
  ] = getApplicationWithIndividualsSeq(
    applicationFilter = Filters.eq(
      fieldName = FieldNames.applicationReference,
      value = applicationReference.value
    ),
    individualForAllFilter = Filters.empty()
  ).map(_.headOption)
//
//  def findRiskedApplicationsWithIndividuals(): Future[
//    Seq[ApplicationWithIndividuals]
//  ] = getApplicationWithIndividualsSeq(
//    applicationFilter = Filters.and(
//      Filters.exists(FieldNames.entityRiskingResult, true)
//    ),
//    individualForAllFilter = Filters.exists(FieldNames.individualRiskingResult, true)
//  )

  def findApprovedNotSubscribed(): Future[
    Seq[ApplicationWithIndividuals]
  ] = getApplicationWithIndividualsSeq(
    applicationFilter = Filters.and(
      Filters.size(FieldNames.entityRiskingResult_failures, 0),
      Filters.eq(FieldNames.isSubscribed, false)
    ),
    individualForAllFilter = Filters.size(FieldNames.individualRiskingResult_failures, 0)
  )

  def findRiskedFailed(): Future[
    Seq[ApplicationWithIndividuals]
  ] = getApplicationWithIndividualsSeq(
    applicationFilter = Filters.and(
      Filters.not(Filters.size(FieldNames.entityRiskingResult_failures, 0)),
      Filters.eq(FieldNames.isSubscribed, false)
    ),
    individualForAllFilter = Filters.size(FieldNames.individualRiskingResult_failures, 0)
  )

  private def getApplicationWithIndividualsSeq(
    applicationFilter: Bson,
    individualForAllFilter: Bson // the filter must apply "forall" individuals otherwise entire ApplicationWithIndividuals is discarded
  ): Future[Seq[ApplicationWithIndividuals]] = collection
    .aggregate[Document](Seq(
      Aggregates.filter(applicationFilter),
      Aggregates.lookup(
        from = "individual-for-risking",
        localField = FieldNames.applicationReference,
        foreignField = FieldNames.applicationReference,
        as = "individuals"
      ),
      Aggregates.filter(Repo.forall("individuals", individualForAllFilter))
    ))
    .toFuture()
    .map:
      _.map: (doc: Document) =>
        val json = Json.parse(doc.toJson(relaxedJson))
        val app = json.as[ApplicationForRisking]
        val individuals = (json \ "individuals").as[Seq[IndividualForRisking]]
        ApplicationWithIndividuals(app, individuals)

  def findReadyForSubmission(): Future[Seq[ApplicationForRisking]] = collection
    .find(Filters.exists(FieldNames.riskingFileName, false)) // ready for submissions don't have set riskingFileId
    .toFuture()

  // TODO needs testing?
  def updateRiskingFileId(
    applicationReferences: Seq[ApplicationReference],
    riskingFileName: RiskingFileName
  ): Future[UpdateResult] = collection
    .updateMany(
      Filters.in(FieldNames.applicationReference, applicationReferences.map(_.value)*),
      Updates.combine(
        Updates.set(FieldNames.riskingFileName, riskingFileName.value),
        Updates.set(FieldNames.lastUpdatedAt, Instant.now(clock).toString)
      )
    ).toFuture()

//  def findReadyForSubscription(): Future[Seq[ApplicationForRisking]] = collection
//    .find(
//      Filters.and(
//        Filters.size("failures", 0),
//        Filters.eq("isSubscribed", false)
//      )
//    ).toFuture()

//  def findNotSubscribedWithResults(): Future[Seq[ApplicationForRisking]] = {
//    collection
//      .find(
//        Filters.and(
//          Filters.exists(FieldNames.entityRiskingResult),
//          Filters.eq(FieldNames.isSubscribed, false)
//        )
//      ).toFuture()
//  }

// when named ApplicationForRiskingRepo, Scala 3 compiler complains
// about cyclic reference error during compilation ...
object ApplicationForRiskingRepoHelp:

  given IdString[ApplicationReference] =
    new IdString[ApplicationReference]:
      override def idString(id: ApplicationReference): String = id.value
      override def idField: String = FieldNames.applicationReference

  given IdExtractor[ApplicationForRisking, ApplicationReference] =
    new IdExtractor[ApplicationForRisking, ApplicationReference]:
      override def id(applicationForRisking: ApplicationForRisking): ApplicationReference = applicationForRisking.applicationReference

  def indexes(cacheTtl: FiniteDuration): Seq[IndexModel] = Seq(
    IndexModel(
      keys = Indexes.ascending(FieldNames.applicationReference),
      indexOptions = IndexOptions()
        .name(FieldNames.applicationReferenceIndex)
        .unique(true)
    ),
    IndexModel(
      keys = Indexes.ascending(FieldNames.riskingFileName),
      indexOptions = IndexOptions()
        .name(FieldNames.riskingFileNameIndex)
        .unique(false)
    ),
    IndexModel(
      keys = Indexes.ascending(FieldNames.lastUpdatedAt),
      indexOptions = IndexOptions()
        .expireAfter(cacheTtl.toSeconds, TimeUnit.SECONDS)
        .name(FieldNames.lastUpdatedAtIndex)
    )
  )
