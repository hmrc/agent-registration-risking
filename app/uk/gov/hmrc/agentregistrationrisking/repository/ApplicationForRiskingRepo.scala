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
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.*
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.model.*
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepoHelp.given
import uk.gov.hmrc.agentregistrationrisking.repository.Repo.IdExtractor
import uk.gov.hmrc.agentregistrationrisking.repository.Repo.IdString
import uk.gov.hmrc.agentregistrationrisking.repository.Repo.toBison
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs

import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

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
  extraCodecs = Seq(
    Codecs.playFormatCodec(ApplicationForRisking.format),
    Codecs.playFormatCodec(RiskingOutcome.format)
  ),
  replaceIndexes = true
):

  // ═══════════════════════════════════════════════════════════════════════════════
  //  CRITICAL: ALL QUERIES MUST BE TESTED IN REPOSITORY SPEC
  //  Untested queries can cause Production data corruption/loss and Difficult recovery !!!!!!!!!!
  // ═══════════════════════════════════════════════════════════════════════════════

  def findReadyForSubmission(): Future[Seq[ApplicationForRisking]] = collection
    .find(Filters.exists(FieldNames.riskingFileName, false)) // ready for submissions don't have set riskingFileId
    .toFuture()

  def findReadyToBeSubscribed(): Future[Seq[ApplicationForRisking]] = collection
    .find(
      Filters.and(
        Filters.eq(FieldNames.overallStatus.riskingOutcome, RiskingOutcome.Approved.toBison),
        Filters.eq(FieldNames.isSubscribed, false)
      )
    )
    .toFuture()

  def findReadyToSetRiskingOutcome(): Future[Seq[ApplicationWithIndividuals]] = findApplicationWithIndividuals(
    applicationFilter = Filters.and(
      Filters.exists(FieldNames.entityRiskingResult),
      Filters.exists(FieldNames.overallStatus.riskingOutcome, false)
    ),
    individualForAllFilter = Filters.exists(FieldNames.individualRiskingResult)
  )

  // ═══════════════════════════════════════════════════════════════════════════════
  //  CRITICAL: ALL QUERIES MUST BE TESTED IN REPOSITORY SPEC
  //  Untested queries can cause Production data corruption/loss and Difficult recovery !!!!!!!!!!
  // ═══════════════════════════════════════════════════════════════════════════════

  def findRequiringEmailProcessingForFailedNonFixable(): Future[Seq[ApplicationWithIndividuals]] = findApplicationWithIndividuals(
    applicationFilter = Filters.and(
      Filters.eq(FieldNames.overallStatus.riskingOutcome, RiskingOutcome.FailedNonFixable.toBison),
      Filters.eq(FieldNames.overallStatus.emailsProcessed, false)
    )
  )

  def findApplicationsAwaitingOverallOutcome(): Future[Seq[ApplicationWithIndividuals]] = findApplicationWithIndividuals(
    applicationFilter = Filters.and(
      Filters.exists(FieldNames.entityRiskingResult),
      Filters.exists(FieldNames.overallStatus.riskingOutcome, false)
    ),
    individualForAllFilter = Filters.exists(FieldNames.individualRiskingResult)
  )

  // ═══════════════════════════════════════════════════════════════════════════════
  //  CRITICAL: ALL QUERIES MUST BE TESTED IN REPOSITORY SPEC
  //  Untested queries can cause Production data corruption/loss and Difficult recovery !!!!!!!!!!
  // ═══════════════════════════════════════════════════════════════════════════════

  private val relaxedJson: JsonWriterSettings = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build()

  private def findApplicationWithIndividuals(
    applicationFilter: Bson,
    individualForAllFilter: Bson = Filters.empty() // the filter must apply "forall" individuals otherwise entire ApplicationWithIndividuals is discarded
  ): Future[Seq[ApplicationWithIndividuals]] = collection
    .aggregate[Document](Seq(
      Aggregates.filter(applicationFilter),
      Aggregates.lookup(
        from = IndividualForRiskingRepo.collectionName,
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

  def updateRiskingFileName(
    applicationReferences: Seq[ApplicationReference],
    riskingFileName: RiskingFileName
  ): Future[Unit] = collection
    .updateMany(
      Filters.in(FieldNames.applicationReference, applicationReferences.map(_.value)*),
      Updates.combine(
        Updates.set(FieldNames.riskingFileName, riskingFileName.value)
      )
    )
    .toFuture()
    .map(_ => ())

  // ═══════════════════════════════════════════════════════════════════════════════
  //  CRITICAL: ALL QUERIES MUST BE TESTED IN REPOSITORY SPEC
  //  Untested queries can cause Production data corruption/loss and Difficult recovery !!!!!!!!!!
  // ═══════════════════════════════════════════════════════════════════════════════

  def findByRiskingFileName(
    riskingFileName: RiskingFileName
  ): Future[Seq[ApplicationForRisking]] = collection
    .find(Filters.eq(FieldNames.riskingFileName, riskingFileName.value))
    .toFuture()

  def findSubscribedReadyForSuccessEmail(): Future[Seq[ApplicationForRisking]] = collection
    .find(
      Filters.and(
        Filters.eq(FieldNames.overallStatus.riskingOutcome, RiskingOutcome.Approved.toBison),
        Filters.eq(FieldNames.isSubscribed, true),
        Filters.eq(FieldNames.isEmailSent, false)
      )
    ).toFuture()

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
    // covers findReadyToBeSubscribed (riskingOutcome + isSubscribed prefix)
    // and   findSubscribedReadyForSuccessEmail (full compound)
    // partial filter keeps index small: only docs that already have a riskingOutcome
    IndexModel(
      keys = Indexes.ascending(
        FieldNames.overallStatus.riskingOutcome,
        FieldNames.isSubscribed,
        FieldNames.isEmailSent
      ),
      indexOptions = IndexOptions()
        .name(FieldNames.subscriptionStatusIndex)
        .partialFilterExpression(Filters.exists(FieldNames.overallStatus.riskingOutcome))
    ),
    // covers findRequiringEmailProcessingForFailedNonFixable (riskingOutcome + emailsProcessed)
    IndexModel(
      keys = Indexes.ascending(FieldNames.overallStatus.riskingOutcome, FieldNames.overallStatus.emailsProcessed),
      indexOptions = IndexOptions()
        .name(FieldNames.emailProcessingStatusIndex)
        .partialFilterExpression(Filters.exists(FieldNames.overallStatus.riskingOutcome))
    ),
    // covers findReadyToSetRiskingOutcome / findApplicationsAwaitingOverallOutcome
    // (entityRiskingResult exists && riskingOutcome NOT exists)
    // partial filter restricts index to docs that have entityRiskingResult but no riskingOutcome yet
    IndexModel(
      keys = Indexes.ascending(FieldNames.entityRiskingResult),
      indexOptions = IndexOptions()
        .name(FieldNames.entityRiskingResultIndex)
        .partialFilterExpression(Filters.and(
          Filters.exists(FieldNames.entityRiskingResult),
          Filters.exists(FieldNames.overallStatus.riskingOutcome, false)
        ))
    )
  )
