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

import uk.gov.hmrc.agentregistrationrisking.model.ApplicationReference
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec

class RiskingFileServiceSpec
extends ISpec:

  val repo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  val service = new RiskingFileService(repo)

  "buildRiskingFile retrieves all applications ready for risking and creates risking file in correct format" in:

    val upsert = repo.upsert(tdAll.llpApplicationForRisking.copy(individuals =
      List(
        tdAll.readyForSubmissionIndividual,
        tdAll.readyForSubmissionIndividual,
        tdAll.readyForSubmissionIndividual
      )
    ))
    upsert.futureValue

    val result: String = service.buildRiskingFile.futureValue
    val recordCount = result.substring(result.length - 1).toInt
    recordCount shouldBe 4
    val pipeCount = result.count(_ == '|')
    pipeCount shouldBe 109
