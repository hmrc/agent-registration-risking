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

import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates

class ApplicationForRiskingRepoSpec
extends ISpec:

  "findReadyForSubmission should return all applications which don't have riskingFileId, which means they aren't submitted to minerva yet" in:
    val applications: Seq[ApplicationForRisking] =
      applicationForRiskingRepo
        .findReadyForSubmission()
        .futureValue
    applications.toSet shouldBe Set(TdRiskingInstancesInStates.readyForSubmission.application)

  "findReadyToBeSubscribed should return all applications which are approved but not subscribed yet" in:

    // THEN
    val applications: Seq[ApplicationForRisking] =
      applicationForRiskingRepo
        .findReadyToBeSubscribed()
        .futureValue

    applications.toSet shouldBe Set(TdRiskingInstancesInStates.approved.application)
    applications.contains(TdRiskingInstancesInStates.approvedAndSubscribed.application) shouldBe false
    applications.contains(TdRiskingInstancesInStates.approvedAndSubscribedAndEmailSent.application) shouldBe false

  val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  override protected def beforeAll(): Unit =
    super.beforeAll()

    def primeDbWithBackgroundData(): Unit =
      applicationForRiskingRepo.collection.drop().toFuture.futureValue
      individualForRiskingRepo.collection.drop().toFuture.futureValue
      tdAll.tdRiskingInstancesInStates.all.foreach: td =>
        applicationForRiskingRepo.upsert(td.application).futureValue
        individualForRiskingRepo.upsert(td.individual1).futureValue
        individualForRiskingRepo.upsert(td.individual2).futureValue

    primeDbWithBackgroundData()
