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

import org.mongodb.scala.ObservableFuture
import org.mongodb.scala.SingleObservableFuture
import org.scalatest.matchers.should.Matchers.shouldBe
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec

class ApplicationForRiskingRepoUnsetFilenameSpec
extends ISpec:

  "unsetFileName sets file name to be None" in:
    val applicationsInDb: Set[ApplicationForRisking] =
      tdAll
        .tdRiskingInstancesInStates
        .all
        .map(_.application)
        .toSet

    applicationForRiskingRepo
      .collection
      .find()
      .toFuture
      .futureValue
      .toSet shouldBe applicationsInDb

    applicationForRiskingRepo
      .unsetFileName()
      .futureValue

    applicationForRiskingRepo
      .collection
      .find()
      .toFuture
      .futureValue
      .toSet shouldBe applicationsInDb
      .map(_.copy(riskingFileName = None))

  private val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  private val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  override protected def beforeAll(): Unit =
    super.beforeAll()
    primeDb()
    ()

  private def primeDb(): Unit =
    applicationForRiskingRepo.collection.drop().toFuture.futureValue
    individualForRiskingRepo.collection.drop().toFuture.futureValue
    tdAll
      .tdRiskingInstancesInStates
      .all
      .foreach: td =>
        applicationForRiskingRepo.upsert(td.application).futureValue
        individualForRiskingRepo.upsert(td.individual1).futureValue
        individualForRiskingRepo.upsert(td.individual2).futureValue
