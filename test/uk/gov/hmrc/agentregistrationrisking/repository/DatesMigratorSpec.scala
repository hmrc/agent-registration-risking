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
import org.bson.BsonValue
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Updates
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.CompletedRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFile
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates

import scala.concurrent.duration.*

class DatesMigratorSpec
extends ISpec:

  "migrate converts the dates of an application without risking results and adds none" in:
    val application: ApplicationForRisking = notRisked.application
    storeWithIsoStringDates(application)

    migrator.migrate().futureValue shouldBe 1L

    dateTypes(rawApplication(application)) shouldBe Map(
      "createdAt" -> BsonType.DATE_TIME,
      "lastUpdatedAt" -> BsonType.DATE_TIME
    )
    rawApplication(application).containsKey("entityRiskingResult") shouldBe false
    applicationRepo.findById(application.applicationReference).futureValue.value shouldBe application

  "migrate converts every date of a risked application, the nested ones included" in:
    val application: ApplicationForRisking = risked.application
    storeWithIsoStringDates(application)

    migrator.migrate().futureValue shouldBe 1L

    dateTypes(rawApplication(application)) shouldBe Map(
      "createdAt" -> BsonType.DATE_TIME,
      "lastUpdatedAt" -> BsonType.DATE_TIME,
      "entityRiskingResult.receivedAt" -> BsonType.DATE_TIME,
      "overallStatus.emailsSentAt" -> BsonType.DATE_TIME
    )
    applicationRepo.findById(application.applicationReference).futureValue.value shouldBe application

  "migrate converts the dates of individuals with and without a risking result" in:
    val individualWithoutResult: IndividualForRisking = notRisked.individual1
    val individualWithResult: IndividualForRisking = risked.individual1
    storeWithIsoStringDates(individualWithoutResult)
    storeWithIsoStringDates(individualWithResult)

    migrator.migrate().futureValue shouldBe 2L

    dateTypes(rawIndividual(individualWithoutResult)) shouldBe Map(
      "createdAt" -> BsonType.DATE_TIME,
      "lastUpdatedAt" -> BsonType.DATE_TIME
    )
    rawIndividual(individualWithoutResult).containsKey("individualRiskingResult") shouldBe false
    dateTypes(rawIndividual(individualWithResult)) shouldBe Map(
      "createdAt" -> BsonType.DATE_TIME,
      "lastUpdatedAt" -> BsonType.DATE_TIME,
      "individualRiskingResult.receivedAt" -> BsonType.DATE_TIME
    )
    individualRepo.findById(individualWithoutResult.personReference).futureValue.value shouldBe individualWithoutResult
    individualRepo.findById(individualWithResult.personReference).futureValue.value shouldBe individualWithResult

  "migrate converts the date of a risking file" in:
    val riskingFile: RiskingFile = risked.tdRisking.riskingFile
    storeWithIsoStringDates(riskingFile)

    migrator.migrate().futureValue shouldBe 1L

    dateTypes(rawRiskingFile(riskingFile)) shouldBe Map("uploadedAt" -> BsonType.DATE_TIME)
    riskingFileRepo.findById(riskingFile.riskingFileName).futureValue.value shouldBe riskingFile

  "migrate converts every date of a completed risking, the individuals' included" in:
    val completedRisking: CompletedRisking = risked.completedRisking
    storeWithIsoStringDates(completedRisking)

    migrator.migrate().futureValue shouldBe 1L

    dateTypes(rawCompletedRisking(completedRisking)) shouldBe completedRiskingDatePaths.map(_ -> BsonType.DATE_TIME).toMap
    completedRiskingRepo.findById(completedRisking.completedRiskingId).futureValue.value shouldBe completedRisking

  "migrate adds no risking file or individuals to a completed risking that has none" in:
    val completedRisking: CompletedRisking = risked.completedRisking.copy(riskingFile = None, individuals = Seq.empty)
    storeWithIsoStringDates(completedRisking)

    migrator.migrate().futureValue shouldBe 1L

    rawCompletedRisking(completedRisking).containsKey("riskingFile") shouldBe false
    rawCompletedRisking(completedRisking).getArray("individuals").isEmpty shouldBe true
    completedRiskingRepo.findById(completedRisking.completedRiskingId).futureValue.value shouldBe completedRisking

  "migrate converts nothing when every date is a BSON date already" in:
    applicationRepo.upsert(risked.application).futureValue
    riskingFileRepo.upsert(risked.tdRisking.riskingFile).futureValue

    migrator.migrate().futureValue shouldBe 0L

  "migrate keeps only the milliseconds of a date stored with more precision, as a BSON date holds no more" in:
    val riskingFile: RiskingFile = risked.tdRisking.riskingFile
    storeWithIsoStringDates(riskingFile.copy(uploadedAt = riskingFile.uploadedAt.plusNanos(123456L)))

    migrator.migrate().futureValue shouldBe 1L

    riskingFileRepo.findById(riskingFile.riskingFileName).futureValue.value shouldBe riskingFile

  "migrate leaves a date it cannot convert and converts the others" in:
    val application: ApplicationForRisking = notRisked.application
    storeWithIsoStringDates(application)
    applicationRepo
      .collection
      .updateOne(Filters.eq("applicationReference", application.applicationReference.value), Updates.set("lastUpdatedAt", "not-a-date"))
      .toFuture()
      .futureValue

    migrator.migrate().futureValue shouldBe 1L

    dateTypes(rawApplication(application)) shouldBe Map(
      "createdAt" -> BsonType.DATE_TIME,
      "lastUpdatedAt" -> BsonType.STRING
    )

  "migrate converts the other collections when converting the applications fails" in:
    val application: ApplicationForRisking = notRisked.application
    val riskingFile: RiskingFile = risked.tdRisking.riskingFile
    storeWithIsoStringDates(application)
    storeWithIsoStringDates(riskingFile)
    makeApplicationsUpdateFail()

    migrator.migrate().futureValue shouldBe 1L

    dateTypes(rawApplication(application)) shouldBe Map(
      "createdAt" -> BsonType.STRING,
      "lastUpdatedAt" -> BsonType.STRING
    )
    dateTypes(rawRiskingFile(riskingFile)) shouldBe Map("uploadedAt" -> BsonType.DATE_TIME)

  "migrate pauses between the runs on a collection" in:
    val startedAt: Long = System.nanoTime()

    migrator.migrate().futureValue shouldBe 0L

    val elapsed: FiniteDuration = (System.nanoTime() - startedAt).nanos
    // all four collections are empty: two quiet runs each, so one pause each
    elapsed should be >= appConfig.DatesMigrator.delayBetweenRuns * 4

  override def beforeEach(): Unit =
    super.beforeEach()
    dropDatabase()

  private lazy val applicationRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  private lazy val individualRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]
  private lazy val riskingFileRepo: RiskingFileRepo = app.injector.instanceOf[RiskingFileRepo]
  private lazy val completedRiskingRepo: CompletedRiskingRepo = app.injector.instanceOf[CompletedRiskingRepo]
  private lazy val migrator: DatesMigrator = app.injector.instanceOf[DatesMigrator]
  private lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  private val notRisked: TdRiskingInstancesInStates.readyForSubmission.type = TdRiskingInstancesInStates.readyForSubmission
  private val risked: TdRiskingInstancesInStates.failedFixableAfterBackendNotified.type = TdRiskingInstancesInStates.failedFixableAfterBackendNotified

  private val applicationDatePaths: Seq[String] = Seq(
    "createdAt",
    "lastUpdatedAt",
    "entityRiskingResult.receivedAt",
    "overallStatus.emailsSentAt"
  )

  private val individualDatePaths: Seq[String] = Seq(
    "createdAt",
    "lastUpdatedAt",
    "individualRiskingResult.receivedAt"
  )

  private val completedRiskingDatePaths: Seq[String] =
    Seq("completedAt", "riskingFile.uploadedAt") ++
      applicationDatePaths.map(path => s"application.$path") ++
      individualDatePaths.map(path => s"individuals.0.$path") ++
      individualDatePaths.map(path => s"individuals.1.$path")

  // stores the record as written before the migration: with the companion format, which writes dates as ISO strings
  private def storeWithIsoStringDates(application: ApplicationForRisking): Unit = insertRaw(applicationRepo.collection, Json.toJsObject(application))

  private def storeWithIsoStringDates(individual: IndividualForRisking): Unit = insertRaw(individualRepo.collection, Json.toJsObject(individual))

  private def storeWithIsoStringDates(riskingFile: RiskingFile): Unit = insertRaw(riskingFileRepo.collection, Json.toJsObject(riskingFile))

  private def storeWithIsoStringDates(completedRisking: CompletedRisking): Unit = insertRaw(completedRiskingRepo.collection, Json.toJsObject(completedRisking))

  private def insertRaw(
    collection: MongoCollection[?],
    json: JsObject
  ): Unit =
    collection
      .withDocumentClass[BsonDocument]()
      .insertOne(BsonDocument.parse(json.toString))
      .toFuture()
      .futureValue
    ()

  private def makeApplicationsUpdateFail(): Unit =
    mongoDatabase
      .runCommand(BsonDocument.parse(s"""{ "collMod": "${ApplicationForRiskingRepo.collectionName}", "validator": { "createdAt": { "$$type": "string" } } }"""))
      .toFuture()
      .futureValue
    ()

  private def rawApplication(application: ApplicationForRisking): BsonDocument = rawDocument(
    collection = applicationRepo.collection,
    idField = "applicationReference",
    id = application.applicationReference.value
  )

  private def rawIndividual(individual: IndividualForRisking): BsonDocument = rawDocument(
    collection = individualRepo.collection,
    idField = "personReference",
    id = individual.personReference.value
  )

  private def rawRiskingFile(riskingFile: RiskingFile): BsonDocument = rawDocument(
    collection = riskingFileRepo.collection,
    idField = "riskingFileName",
    id = riskingFile.riskingFileName.value
  )

  private def rawCompletedRisking(completedRisking: CompletedRisking): BsonDocument = rawDocument(
    collection = completedRiskingRepo.collection,
    idField = "_id",
    id = completedRisking.completedRiskingId.value
  )

  private def rawDocument(
    collection: MongoCollection[?],
    idField: String,
    id: String
  ): BsonDocument =
    collection
      .withDocumentClass[BsonDocument]()
      .find(Filters.eq(idField, id))
      .headOption()
      .futureValue
      .value

  private def dateTypes(document: BsonDocument): Map[String, BsonType] =
    (applicationDatePaths ++ individualDatePaths ++ Seq("uploadedAt") ++ completedRiskingDatePaths)
      .distinct
      .flatMap(path => rawValue(document, path).map(value => path -> value.getBsonType))
      .toMap

  private def rawValue(
    document: BsonDocument,
    path: String
  ): Option[BsonValue] =
    path.split('.').foldLeft(Option[BsonValue](document)): (value, name) =>
      value.flatMap: parent =>
        if parent.isDocument
        then Option(parent.asDocument().get(name))
        else if parent.isArray
        then name.toIntOption.filter(_ < parent.asArray().size).map(parent.asArray().get)
        else None
