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

package uk.gov.hmrc.agentregistrationrisking.runner

import play.api.mvc.Request
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFile
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileName
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileWithContent
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.RiskingFileRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec

class RiskingRunnerSpec
extends ISpec:

  "build risking file" in:
    given request: Request[?] = tdAll.backendRequest
    val (riskingFileWithContent: RiskingFileWithContent, applicationReferences: Seq[ApplicationReference]) = riskingRunner.buildRiskingFile().futureValue

    riskingFileWithContent.riskingFile shouldBe RiskingFile(
      riskingFileName = RiskingFileName("asa_risking_file_version1_0_4_20591125_163351.txt"),
      uploadedAt = tdAll.instant
    )

    applicationReferences shouldBe List(
      tdAll.tdRiskingInstancesInStates.readyForSubmission.application.applicationReference,
      tdAll.tdRiskingInstancesInStates.readyForSubmission2.application.applicationReference
    )

    riskingFileWithContent.riskingFileContent shouldBe
      """00|ARR|SAS|20591125|163351
        |01|Entity|N|APPGENPAR1|Alice Smith|(+44) 10794554342|user@test.com|GeneralPartnership|1234567895||||HMRC|XAML00000123456||None|||||||||||
        |01|Entity|N|APPREF_readyForSubmission2|Alice Smith|(+44) 10794554342|user@test.com|LimitedLiabilityPartnership|1234567895|1234567890|123456789|123/AB12345|HMRC|XAML00000123456||None|||||||||||
        |01|Individual|N||||||||123456789|123/AB12345||||None|PREFGENP01|||Test Name|01-01-2000|AB123456C|1234567895|(+44) 10794554342|member@test.com|N|Y
        |01|Individual|N||||||||123456789|123/AB12345||||None|PREFGENP02|||Test Name|01-01-2000|AB123456C|1234567895|(+44) 10794554342|member@test.com|N|Y
        |01|Individual|N||||||||123456789|123/AB12345||||None|PREF_readyForSubmission201|||Test Name|01-01-2000|AB123456C|1234567895|(+44) 10794554342|member@test.com|N|Y
        |01|Individual|N||||||||123456789|123/AB12345||||None|PREF_readyForSubmission202|||Test Name|01-01-2000|AB123456C|1234567895|(+44) 10794554342|member@test.com|N|Y
        |99|6
        |""".stripMargin

  override def beforeEach(): Unit =
    super.beforeEach()
    primeDb()

  val riskingRunner: RiskingRunner = app.injector.instanceOf[RiskingRunner]

  val riskingFileRepo: RiskingFileRepo = app.injector.instanceOf[RiskingFileRepo]
  val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  private def primeDb(): Unit =
    dropDatabase()
//    riskingFileRepo.collection.drop().toFuture.futureValue
//    applicationForRiskingRepo.collection.drop().toFuture.futureValue
//    individualForRiskingRepo.collection.drop().toFuture.futureValue
    tdAll
      .tdRiskingInstancesInStates
      .all
      .foreach: td =>
        applicationForRiskingRepo.upsert(td.application).futureValue
        individualForRiskingRepo.upsert(td.individual1).futureValue
        individualForRiskingRepo.upsert(td.individual2).futureValue
