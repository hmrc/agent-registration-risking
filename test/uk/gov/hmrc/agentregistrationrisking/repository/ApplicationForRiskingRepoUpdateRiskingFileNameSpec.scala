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
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileName
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates

class ApplicationForRiskingRepoUpdateRiskingFileNameSpec
extends ISpec:

  "updateRiskingFileId for one application" in:

    val application: ApplicationForRisking = tdAll.tdRiskingInstancesInStates.readyForSubmission.application
    application.riskingFileName shouldBe None

    val riskingFileName: RiskingFileName = RiskingFileName("risking-file-name-1234")

    applicationForRiskingRepo.findById(application.applicationReference).futureValue shouldBe Some(application)
    applicationForRiskingRepo.findByRiskingFileName(riskingFileName).futureValue shouldBe Seq.empty

    applicationForRiskingRepo
      .updateRiskingFileName(
        applicationReferences = Seq(application.applicationReference),
        riskingFileName = riskingFileName
      )
      .futureValue

    val applicationForRiskingWithUpdatedRiskingFileName: ApplicationForRisking = application.copy(riskingFileName = Some(riskingFileName))

    applicationForRiskingRepo.findByRiskingFileName(riskingFileName).futureValue shouldBe Seq(applicationForRiskingWithUpdatedRiskingFileName)
    applicationForRiskingRepo.findById(application.applicationReference).futureValue shouldBe Some(applicationForRiskingWithUpdatedRiskingFileName)

  "updateRiskingFileId for many applications" in:

    val application1: ApplicationForRisking = tdAll.tdRiskingInstancesInStates.readyForSubmission.application
    application1.riskingFileName shouldBe None
    val application2: ApplicationForRisking = tdAll.tdRiskingInstancesInStates.readyForSubmission2.application
    application2.riskingFileName shouldBe None

    val riskingFileName: RiskingFileName = RiskingFileName("risking-file-name-1234")

    applicationForRiskingRepo.findById(application1.applicationReference).futureValue shouldBe Some(application1)
    applicationForRiskingRepo.findById(application2.applicationReference).futureValue shouldBe Some(application2)
    applicationForRiskingRepo.findByRiskingFileName(riskingFileName).futureValue shouldBe Seq.empty

    applicationForRiskingRepo
      .updateRiskingFileName(
        applicationReferences = Seq(
          application1.applicationReference,
          application2.applicationReference
        ),
        riskingFileName = riskingFileName
      )
      .futureValue

    val application1WithUpdatedRiskingFileName: ApplicationForRisking = application1.copy(riskingFileName = Some(riskingFileName))
    val application2WithUpdatedRiskingFileName: ApplicationForRisking = application2.copy(riskingFileName = Some(riskingFileName))

    applicationForRiskingRepo.findByRiskingFileName(riskingFileName).futureValue shouldBe Seq(
      application1WithUpdatedRiskingFileName,
      application2WithUpdatedRiskingFileName
    )
    applicationForRiskingRepo.findById(application1.applicationReference).futureValue shouldBe Some(application1WithUpdatedRiskingFileName)
    applicationForRiskingRepo.findById(application2.applicationReference).futureValue shouldBe Some(application2WithUpdatedRiskingFileName)

  private val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  private val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  override def beforeEach(): Unit =
    super.beforeEach()
    primeDb()

  private def primeDb(): Unit =
    applicationForRiskingRepo.collection.drop().toFuture.futureValue
    individualForRiskingRepo.collection.drop().toFuture.futureValue
    tdAll
      .tdRiskingInstancesInStates
      .all
      .foreach: td =>
        applicationForRiskingRepo.upsert(td.application).futureValue
        td.individuals.foreach(individualForRiskingRepo.upsert(_).futureValue)
