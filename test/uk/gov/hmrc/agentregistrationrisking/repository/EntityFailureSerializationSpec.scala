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

import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRiskingId
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRiskingId
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.*

class EntityFailureSerializationSpec
extends ISpec:

  val repo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  val individualRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  "EntityFailure serialization round-trip through MongoDB" - {

    "case objects (no value) should survive round-trip" in {
      val entityFailures = List(
        EntityFailure._3._1,
        EntityFailure._3._2,
        EntityFailure._4._1,
        EntityFailure._4._3,
        EntityFailure._8._5,
        EntityFailure._8._7
      )

      val application = tdAll.llpApplicationForRisking.copy(
        _id = ApplicationForRiskingId("entity-test-1"),
        failures = Some(entityFailures)
      )
      repo.upsert(application).futureValue

      val retrieved = repo.findById(application._id).futureValue.value
      retrieved.failures.value should contain theSameElementsAs entityFailures
    }

    "case classes with value field should survive round-trip" in {
      val entityFailures = List(
        EntityFailure._5._1(1234.56),
        EntityFailure._5._2(500.00),
        EntityFailure._5._3(999.99)
      )

      val application = tdAll.llpApplicationForRisking.copy(
        _id = ApplicationForRiskingId("entity-test-2"),
        failures = Some(entityFailures)
      )
      repo.upsert(application).futureValue

      val retrieved = repo.findById(application._id).futureValue.value
      retrieved.failures.value should contain theSameElementsAs entityFailures
    }

    "mixed fixable and non-fixable failures should survive round-trip" in {
      val entityFailures: List[EntityFailure] = List(
        EntityFailure._3._2,
        EntityFailure._5._1(2500.00),
        EntityFailure._7,
        EntityFailure._8._1
      )

      val application = tdAll.llpApplicationForRisking.copy(
        _id = ApplicationForRiskingId("entity-test-3"),
        failures = Some(entityFailures)
      )
      repo.upsert(application).futureValue

      val retrieved = repo.findById(application._id).futureValue.value
      retrieved.failures.value should contain theSameElementsAs entityFailures
    }

    "empty failures list should survive round-trip" in {
      val application = tdAll.llpApplicationForRisking.copy(
        _id = ApplicationForRiskingId("entity-test-4"),
        failures = Some(List.empty)
      )
      repo.upsert(application).futureValue

      val retrieved = repo.findById(application._id).futureValue.value
      retrieved.failures.value shouldBe List.empty
    }

    "IndividualFailure should survive round-trip" in {
      val individualFailures = List(
        IndividualFailure._4._1,
        IndividualFailure._4._3,
        IndividualFailure._5._1(750.00),
        IndividualFailure._9
      )

      val individual = tdAll.readyForSubmissionIndividual(ApplicationForRiskingId("ind-test-app")).copy(
        _id = IndividualForRiskingId("ind-test-1"),
        failures = Some(individualFailures)
      )
      individualRepo.upsert(individual).futureValue

      val retrieved = individualRepo.findById(individual._id).futureValue.value
      retrieved.failures.value should contain theSameElementsAs individualFailures
    }
  }
