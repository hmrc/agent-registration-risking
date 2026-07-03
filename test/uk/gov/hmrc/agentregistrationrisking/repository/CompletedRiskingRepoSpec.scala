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

import uk.gov.hmrc.agentregistrationrisking.model.CompletedRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import com.softwaremill.quicklens.modify
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference

import java.time.temporal.ChronoUnit

class CompletedRiskingRepoSpec
extends ISpec:

  val completedRiskingRepo: CompletedRiskingRepo = app.injector.instanceOf[CompletedRiskingRepo]

  "find recent should result recently archived completed risking record" in:
    val completedRisking1: CompletedRisking =
      tdAll
        .tdRiskingInstancesInStates
        .failedFixableAfterBackendNotified
        .completedRisking

    val applicationReference: ApplicationReference = completedRisking1.application.applicationReference
    val personReference: PersonReference = completedRisking1.individuals.headOption.value.personReference

    val completedRisking2: CompletedRisking = completedRisking1
      .modify(_.completedAt)
      .using(_.plus(10, ChronoUnit.DAYS))
      .modify(_._id.value)
      .using(_ + "_2")

    withClue("data consistency checks"):
      completedRisking1.completedRiskingId should not be completedRisking2.completedRiskingId
      completedRisking1.application.applicationReference shouldBe applicationReference
      completedRisking2.application.applicationReference shouldBe applicationReference

    completedRiskingRepo.upsert(completedRisking1).futureValue
    completedRiskingRepo.upsert(completedRisking2).futureValue

    completedRiskingRepo
      .findRecent(applicationReference)
      .futureValue
      .value shouldBe completedRisking2

    completedRiskingRepo
      .findRecent(personReference)
      .futureValue
      .value shouldBe completedRisking2
