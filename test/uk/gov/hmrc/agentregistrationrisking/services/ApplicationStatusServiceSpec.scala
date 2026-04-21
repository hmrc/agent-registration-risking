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

package uk.gov.hmrc.agentregistrationrisking.services

import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRiskingId
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRiskingId
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileId
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.*

class ApplicationStatusServiceSpec
extends ISpec:

  val service: ApplicationStatusService = app.injector.instanceOf[ApplicationStatusService]
  val repo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  val individualRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  "getApprovedApplicationsWithIndividuals" - {

    "returns application when entity and all individuals have no failures" in {
      val application = tdAll.llpApplicationForRisking.copy(
        _id = ApplicationForRiskingId("approved-app"),
        riskingFileId = Some(RiskingFileId("file-1")),
        failures = Some(List.empty)
      )
      val individual = tdAll.readyForSubmissionIndividual(application._id).copy(
        _id = IndividualForRiskingId("approved-ind"),
        riskingFileId = Some(RiskingFileId("file-1")),
        failures = Some(List.empty)
      )
      repo.upsert(application).futureValue
      individualRepo.upsert(individual).futureValue

      val result = service.getApprovedApplicationsWithIndividuals.futureValue
      result.size shouldBe 1
      result.headOption.value.application._id shouldBe application._id
    }

    "does not return application when entity has fixable failures" in {
      val application = tdAll.llpApplicationForRisking.copy(
        _id = ApplicationForRiskingId("fixable-app"),
        riskingFileId = Some(RiskingFileId("file-2")),
        failures = Some(List(EntityFailure._3._2))
      )
      val individual = tdAll.readyForSubmissionIndividual(application._id).copy(
        _id = IndividualForRiskingId("fixable-ind"),
        riskingFileId = Some(RiskingFileId("file-2")),
        failures = Some(List.empty)
      )
      repo.upsert(application).futureValue
      individualRepo.upsert(individual).futureValue

      val result = service.getApprovedApplicationsWithIndividuals.futureValue
      result.size shouldBe 0
    }

    "does not return application when individual has fixable failures" in {
      val application = tdAll.llpApplicationForRisking.copy(
        _id = ApplicationForRiskingId("ind-fixable-app"),
        riskingFileId = Some(RiskingFileId("file-3")),
        failures = Some(List.empty)
      )
      val individual = tdAll.readyForSubmissionIndividual(application._id).copy(
        _id = IndividualForRiskingId("ind-fixable-ind"),
        riskingFileId = Some(RiskingFileId("file-3")),
        failures = Some(List(IndividualFailure._4._1))
      )
      repo.upsert(application).futureValue
      individualRepo.upsert(individual).futureValue

      val result = service.getApprovedApplicationsWithIndividuals.futureValue
      result.size shouldBe 0
    }

    "does not return application that is already subscribed" in {
      val application = tdAll.llpApplicationForRisking.copy(
        _id = ApplicationForRiskingId("subscribed-app"),
        riskingFileId = Some(RiskingFileId("file-4")),
        failures = Some(List.empty),
        isSubscribed = true
      )
      val individual = tdAll.readyForSubmissionIndividual(application._id).copy(
        _id = IndividualForRiskingId("subscribed-ind"),
        riskingFileId = Some(RiskingFileId("file-4")),
        failures = Some(List.empty)
      )
      repo.upsert(application).futureValue
      individualRepo.upsert(individual).futureValue

      val result = service.getApprovedApplicationsWithIndividuals.futureValue
      result.size shouldBe 0
    }

    "does not return application when individual has not yet received results" in {
      val application = tdAll.llpApplicationForRisking.copy(
        _id = ApplicationForRiskingId("partial-app"),
        riskingFileId = Some(RiskingFileId("file-5")),
        failures = Some(List.empty)
      )
      val individual = tdAll.readyForSubmissionIndividual(application._id).copy(
        _id = IndividualForRiskingId("partial-ind"),
        riskingFileId = Some(RiskingFileId("file-5")),
        failures = None
      )
      repo.upsert(application).futureValue
      individualRepo.upsert(individual).futureValue

      val result = service.getApprovedApplicationsWithIndividuals.futureValue
      result.size shouldBe 0
    }
  }
