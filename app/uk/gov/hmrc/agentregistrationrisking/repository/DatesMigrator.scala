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

import org.bson.BsonDocument
import org.bson.BsonType
import org.bson.conversions.Bson
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import play.api.Logging
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationrisking.repository.DatesMigrator.*

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

// TODO: remove, with DatesMigratorStarter and the dates-migrator config, once the dates migration has run in every environment
/** Converts the dates that `application-for-risking`, `individual-for-risking`, `risking-file` and `risking-completed` records hold as ISO strings into BSON
  * dates.
  */
@Singleton
class DatesMigrator @Inject() (
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  individualForRiskingRepo: IndividualForRiskingRepo,
  riskingFileRepo: RiskingFileRepo,
  completedRiskingRepo: CompletedRiskingRepo
)(using ExecutionContext)
extends Logging:

  /** @return the number of documents converted; a collection whose migration failed adds none */
  def migrate(): Future[Long] =
    for
      // the TTL indexes on these dates go when the repos initialise; converting a date first would let Mongo expire its record
      _ <- Future.sequence(Seq(
        applicationForRiskingRepo.initialised,
        individualForRiskingRepo.initialised,
        riskingFileRepo.initialised,
        completedRiskingRepo.initialised
      ))
      applications <- runUntilTwoQuietRuns(
        collection = applicationForRiskingRepo.collection,
        filter = applicationsWithStringDates,
        update = applicationDatesConverted
      )
        .map: converted =>
          logger.info(s"Migrating '${ApplicationForRiskingRepo.collectionName}': converted $converted documents in total DONE")
          converted
        .recover:
          case ex =>
            logger.error(s"Migrating '${ApplicationForRiskingRepo.collectionName}': FAILED", ex)
            0L
      individuals <- runUntilTwoQuietRuns(
        collection = individualForRiskingRepo.collection,
        filter = individualsWithStringDates,
        update = individualDatesConverted
      )
        .map: converted =>
          logger.info(s"Migrating '${IndividualForRiskingRepo.collectionName}': converted $converted documents in total DONE")
          converted
        .recover:
          case ex =>
            logger.error(s"Migrating '${IndividualForRiskingRepo.collectionName}': FAILED", ex)
            0L
      riskingFiles <- runUntilTwoQuietRuns(
        collection = riskingFileRepo.collection,
        filter = riskingFilesWithStringDates,
        update = riskingFileDatesConverted
      )
        .map: converted =>
          logger.info(s"Migrating '${RiskingFileRepo.collectionName}': converted $converted documents in total DONE")
          converted
        .recover:
          case ex =>
            logger.error(s"Migrating '${RiskingFileRepo.collectionName}': FAILED", ex)
            0L
      completedRiskings <- runUntilTwoQuietRuns(
        collection = completedRiskingRepo.collection,
        filter = completedRiskingsWithStringDates,
        update = completedRiskingDatesConverted
      )
        .map: converted =>
          logger.info(s"Migrating '${CompletedRiskingRepo.collectionName}': converted $converted documents in total DONE")
          converted
        .recover:
          case ex =>
            logger.error(s"Migrating '${CompletedRiskingRepo.collectionName}': FAILED", ex)
            0L
    yield applications + individuals + riskingFiles + completedRiskings

  private def runUntilTwoQuietRuns(
    collection: MongoCollection[?],
    filter: Bson,
    update: Bson
  ): Future[Long] =
    val collectionName: String = collection.namespace.getCollectionName
    logger.info(s"Migrating '$collectionName': Started...")

    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def run(
      runNumber: Int,
      quietRuns: Int,
      converted: Long
    ): Future[Long] = collection
      .updateMany(filter = filter, update = Seq(update))
      .toFuture()
      .map(_.getModifiedCount)
      .flatMap: convertedInRun =>
        logger.info(s"Migrating '$collectionName': converted $convertedInRun documents in run $runNumber")
        val quietRunsNow: Int = if convertedInRun === 0L then quietRuns + 1 else 0
        if quietRunsNow === 2
        then Future.successful(converted)
        else
          run(
            runNumber = runNumber + 1,
            quietRuns = quietRunsNow,
            converted = converted + convertedInRun
          )

    run(
      runNumber = 1,
      quietRuns = 0,
      converted = 0L
    )

object DatesMigrator:

  private val applicationsWithStringDates: Bson = Filters.or(
    Filters.bsonType("createdAt", BsonType.STRING),
    Filters.bsonType("lastUpdatedAt", BsonType.STRING),
    Filters.bsonType("entityRiskingResult.receivedAt", BsonType.STRING),
    Filters.bsonType("overallStatus.emailsSentAt", BsonType.STRING)
  )

  // A date inside an optional object is set through that object, and only where it exists: setting the dotted field would add an empty object the format cannot read.
  private val applicationDatesConverted: Bson = BsonDocument.parse(
    // language=JSON
    """{ "$set": {
      |  "createdAt": { "$convert": { "input": "$createdAt", "to": "date", "onError": "$createdAt", "onNull": "$createdAt" } },
      |  "lastUpdatedAt": { "$convert": { "input": "$lastUpdatedAt", "to": "date", "onError": "$lastUpdatedAt", "onNull": "$lastUpdatedAt" } },
      |  "entityRiskingResult": { "$cond": [
      |    { "$eq": [{ "$type": "$entityRiskingResult" }, "object"] },
      |    { "$mergeObjects": ["$entityRiskingResult", {
      |      "receivedAt": { "$convert": { "input": "$entityRiskingResult.receivedAt", "to": "date", "onError": "$entityRiskingResult.receivedAt", "onNull": "$entityRiskingResult.receivedAt" } }
      |    }] },
      |    "$entityRiskingResult"
      |  ] },
      |  "overallStatus.emailsSentAt": { "$convert": { "input": "$overallStatus.emailsSentAt", "to": "date", "onError": "$overallStatus.emailsSentAt", "onNull": "$overallStatus.emailsSentAt" } }
      |} }""".stripMargin
  )

  private val individualsWithStringDates: Bson = Filters.or(
    Filters.bsonType("createdAt", BsonType.STRING),
    Filters.bsonType("lastUpdatedAt", BsonType.STRING),
    Filters.bsonType("individualRiskingResult.receivedAt", BsonType.STRING)
  )

  private val individualDatesConverted: Bson = BsonDocument.parse(
    // language=JSON
    """{ "$set": {
      |  "createdAt": { "$convert": { "input": "$createdAt", "to": "date", "onError": "$createdAt", "onNull": "$createdAt" } },
      |  "lastUpdatedAt": { "$convert": { "input": "$lastUpdatedAt", "to": "date", "onError": "$lastUpdatedAt", "onNull": "$lastUpdatedAt" } },
      |  "individualRiskingResult": { "$cond": [
      |    { "$eq": [{ "$type": "$individualRiskingResult" }, "object"] },
      |    { "$mergeObjects": ["$individualRiskingResult", {
      |      "receivedAt": { "$convert": { "input": "$individualRiskingResult.receivedAt", "to": "date", "onError": "$individualRiskingResult.receivedAt", "onNull": "$individualRiskingResult.receivedAt" } }
      |    }] },
      |    "$individualRiskingResult"
      |  ] }
      |} }""".stripMargin
  )

  private val riskingFilesWithStringDates: Bson = Filters.bsonType("uploadedAt", BsonType.STRING)

  private val riskingFileDatesConverted: Bson = BsonDocument.parse(
    // language=JSON
    """{ "$set": {
      |  "uploadedAt": { "$convert": { "input": "$uploadedAt", "to": "date", "onError": "$uploadedAt", "onNull": "$uploadedAt" } }
      |} }""".stripMargin
  )

  private val completedRiskingsWithStringDates: Bson = Filters.or(
    Filters.bsonType("completedAt", BsonType.STRING),
    Filters.bsonType("riskingFile.uploadedAt", BsonType.STRING),
    Filters.bsonType("application.createdAt", BsonType.STRING),
    Filters.bsonType("application.lastUpdatedAt", BsonType.STRING),
    Filters.bsonType("application.entityRiskingResult.receivedAt", BsonType.STRING),
    Filters.bsonType("application.overallStatus.emailsSentAt", BsonType.STRING),
    Filters.bsonType("individuals.createdAt", BsonType.STRING),
    Filters.bsonType("individuals.lastUpdatedAt", BsonType.STRING),
    Filters.bsonType("individuals.individualRiskingResult.receivedAt", BsonType.STRING)
  )

  private val completedRiskingDatesConverted: Bson = BsonDocument.parse(
    // language=JSON
    """{ "$set": {
      |  "completedAt": { "$convert": { "input": "$completedAt", "to": "date", "onError": "$completedAt", "onNull": "$completedAt" } },
      |  "riskingFile": { "$cond": [
      |    { "$eq": [{ "$type": "$riskingFile" }, "object"] },
      |    { "$mergeObjects": ["$riskingFile", {
      |      "uploadedAt": { "$convert": { "input": "$riskingFile.uploadedAt", "to": "date", "onError": "$riskingFile.uploadedAt", "onNull": "$riskingFile.uploadedAt" } }
      |    }] },
      |    "$riskingFile"
      |  ] },
      |  "application.createdAt": { "$convert": { "input": "$application.createdAt", "to": "date", "onError": "$application.createdAt", "onNull": "$application.createdAt" } },
      |  "application.lastUpdatedAt": { "$convert": { "input": "$application.lastUpdatedAt", "to": "date", "onError": "$application.lastUpdatedAt", "onNull": "$application.lastUpdatedAt" } },
      |  "application.entityRiskingResult": { "$cond": [
      |    { "$eq": [{ "$type": "$application.entityRiskingResult" }, "object"] },
      |    { "$mergeObjects": ["$application.entityRiskingResult", {
      |      "receivedAt": { "$convert": { "input": "$application.entityRiskingResult.receivedAt", "to": "date", "onError": "$application.entityRiskingResult.receivedAt", "onNull": "$application.entityRiskingResult.receivedAt" } }
      |    }] },
      |    "$application.entityRiskingResult"
      |  ] },
      |  "application.overallStatus.emailsSentAt": { "$convert": { "input": "$application.overallStatus.emailsSentAt", "to": "date", "onError": "$application.overallStatus.emailsSentAt", "onNull": "$application.overallStatus.emailsSentAt" } },
      |  "individuals": { "$map": { "input": "$individuals", "as": "individual", "in": { "$mergeObjects": ["$$individual", {
      |    "createdAt": { "$convert": { "input": "$$individual.createdAt", "to": "date", "onError": "$$individual.createdAt", "onNull": "$$individual.createdAt" } },
      |    "lastUpdatedAt": { "$convert": { "input": "$$individual.lastUpdatedAt", "to": "date", "onError": "$$individual.lastUpdatedAt", "onNull": "$$individual.lastUpdatedAt" } },
      |    "individualRiskingResult": { "$cond": [
      |      { "$eq": [{ "$type": "$$individual.individualRiskingResult" }, "object"] },
      |      { "$mergeObjects": ["$$individual.individualRiskingResult", {
      |        "receivedAt": { "$convert": { "input": "$$individual.individualRiskingResult.receivedAt", "to": "date", "onError": "$$individual.individualRiskingResult.receivedAt", "onNull": "$$individual.individualRiskingResult.receivedAt" } }
      |      }] },
      |      "$$individual.individualRiskingResult"
      |    ] }
      |  }] } } }
      |} }""".stripMargin
  )
