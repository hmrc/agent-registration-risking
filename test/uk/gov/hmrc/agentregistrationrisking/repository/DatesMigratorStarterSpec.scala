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
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFile
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates
import uk.gov.hmrc.mongo.lock.MongoLockRepository

import scala.concurrent.duration.*

class DatesMigratorStarterSpec
extends ISpec:

  "start runs the dates migrator when no other instance holds the lock" in:
    storeWithUploadedAtAsIsoString(riskingFile)

    starter.start().futureValue shouldBe Some(1L)

    uploadedAtType shouldBe BsonType.DATE_TIME

  "start does nothing while another instance holds the lock" in:
    storeWithUploadedAtAsIsoString(riskingFile)
    mongoLockRepository.takeLock(
      lockId = DatesMigratorStarter.lockId,
      owner = "another-instance",
      ttl = 1.hour
    ).futureValue shouldBe defined

    starter.start().futureValue shouldBe None

    uploadedAtType shouldBe BsonType.STRING

  override def beforeEach(): Unit =
    super.beforeEach()
    dropDatabase()

  private lazy val riskingFileRepo: RiskingFileRepo = app.injector.instanceOf[RiskingFileRepo]
  private lazy val starter: DatesMigratorStarter = app.injector.instanceOf[DatesMigratorStarter]
  private lazy val mongoLockRepository: MongoLockRepository = app.injector.instanceOf[MongoLockRepository]

  private val riskingFile: RiskingFile = TdRiskingInstancesInStates.readyForSubmission.tdRisking.riskingFile

  private def storeWithUploadedAtAsIsoString(riskingFile: RiskingFile): Unit =
    riskingFileRepo
      .collection
      .withDocumentClass[BsonDocument]()
      .insertOne(BsonDocument.parse(Json.toJsObject(riskingFile).toString))
      .toFuture()
      .futureValue
    ()

  private def uploadedAtType: BsonType =
    riskingFileRepo
      .collection
      .withDocumentClass[BsonDocument]()
      .find(Filters.eq("riskingFileName", riskingFile.riskingFileName.value))
      .headOption()
      .futureValue
      .value
      .get("uploadedAt")
      .getBsonType
