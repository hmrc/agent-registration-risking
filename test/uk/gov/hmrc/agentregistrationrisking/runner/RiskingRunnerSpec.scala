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

import play.api.mvc.AnyContent
import play.api.mvc.Request
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.PersonReference
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.services.Crypto
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.randomId
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.StubMaker
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.ObjectStoreStubs

import scala.util.chaining.scalaUtilChainingOps

class RiskingRunnerSpec
extends ISpec:

  "RiskingRunner.run prepares and uploads file to object store" in:

    val riskingRunner: RiskingRunner = app.injector.instanceOf[RiskingRunner]
    val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
    val crypto: Crypto = app.injector.instanceOf[Crypto]

    val personReference1 = PersonReference(randomId)
    val personReference2 = PersonReference(randomId)
    val personReference3 = PersonReference(randomId)

    val applicationForRisking: ApplicationForRisking = tdAll.llpApplicationForRisking.copy(individuals =
      List(
        tdAll.readyForSubmissionIndividual(Some(personReference1)),
        tdAll.readyForSubmissionIndividual(Some(personReference2)),
        tdAll.readyForSubmissionIndividual(Some(personReference3))
      )
    )
    given request: Request[AnyContent] = TdAll.tdAll.fakeBackendRequest
    applicationForRiskingRepo.upsert(applicationForRisking).futureValue

    val fileName: String = "asa_risking_file_version1_0_4_20591125_163351.txt"
    ObjectStoreStubs.stubObjectStoreTransfer(fileName = fileName)

    riskingRunner.run().futureValue shouldBe ()
    ObjectStoreStubs.verifyObjectStoreTransfer(fileName = fileName)

    // TODO: there is problem with test data which is not deterministic and is missing data. (APB-10869)
    ObjectStoreStubs
      .getRequestBody(fileName = fileName)
      .pipe(crypto.decrypt) shouldBe
      s"""00|ARR|SAS|20591125|163351
         |01|Entity|N|${tdAll.llpApplicationForRisking.applicationReference.value}|Test Applicant|01234567890|test@example.com|LimitedLiabilityPartnership|1234567890|OC123456|123456789,123456789|123/AB12345,123/AB12345|HMRC|XAML00000123456|25-11-2059|evidence-reference-123|||||||||||
         |01|Individual|N||||||||123456789,123456789|123/AB12345,123/AB12345|||||${personReference1.value}|||Test Individual|01-01-1980|AA0011221A|1234567890|01234567890|test@example.com|Y|Y
         |01|Individual|N||||||||123456789,123456789|123/AB12345,123/AB12345|||||${personReference2.value}|||Test Individual|01-01-1980|AA0011221A|1234567890|01234567890|test@example.com|Y|Y
         |01|Individual|N||||||||123456789,123456789|123/AB12345,123/AB12345|||||${personReference3.value}|||Test Individual|01-01-1980|AA0011221A|1234567890|01234567890|test@example.com|Y|Y
         |99|4"""
        .stripMargin
