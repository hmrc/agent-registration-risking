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

import play.api.libs.json.Json
import play.api.mvc.AnyContent
import play.api.mvc.Request
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.risking.RiskingStatus
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRiskingId
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileId
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.RiskingFileRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.randomId
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.ObjectStoreStubs
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.SdesProxyStubs

class RiskingRunnerSpec
extends ISpec:

  "RiskingRunner.run prepares and uploads file to object store" in:

    val riskingRunner: RiskingRunner = app.injector.instanceOf[RiskingRunner]
    val repo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
    val riskingFileRepo: RiskingFileRepo = app.injector.instanceOf[RiskingFileRepo]
    val individualRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

    val personReference1 = PersonReference(randomId)
    val personReference2 = PersonReference(randomId)
    val personReference3 = PersonReference(randomId)

    val application = tdAll.llpApplicationForRisking
    given request: Request[AnyContent] = TdAll.tdAll.fakeBackendRequest
    repo.upsert(application).futureValue

    val baseIndividual = tdAll.readyForSubmissionIndividual(application._id)
    val individual1 = baseIndividual.copy(
      _id = IndividualForRiskingId("ind-1"),
      individualProvidedDetails = baseIndividual.individualProvidedDetails.copy(personReference = personReference1)
    )
    val individual2 = baseIndividual.copy(
      _id = IndividualForRiskingId("ind-2"),
      individualProvidedDetails = baseIndividual.individualProvidedDetails.copy(personReference = personReference2)
    )
    val individual3 = baseIndividual.copy(
      _id = IndividualForRiskingId("ind-3"),
      individualProvidedDetails = baseIndividual.individualProvidedDetails.copy(personReference = personReference3)
    )
    individualRepo.upsert(individual1).futureValue
    individualRepo.upsert(individual2).futureValue
    individualRepo.upsert(individual3).futureValue

    val fileName: String = "asa_risking_file_version1_0_4_20591125_163351.txt"
    ObjectStoreStubs.stubObjectStoreTransfer(fileName = fileName)
    ObjectStoreStubs.stubObjectStoreGeneratePresignedUrl(tdAll.objectStoreDirectory, tdAll.fileName)
    SdesProxyStubs.stubSdesFileReady(tdAll.notifySdesFileReadyRequest)

    riskingRunner.run().futureValue shouldBe ()
    ObjectStoreStubs.verifyObjectStoreTransfer(fileName = fileName)

    SdesProxyStubs.getSdesFileReadyRequestBody shouldBe
      Json.stringify(Json.parse(
        s"""{
           |  "informationType":"test-outbound-information-type",
           |  "file":{
           |    "recipientOrSender":"test-srn",
           |    "name":"$fileName",
           |    "location":"http://presigned-url/file",
           |    "checksum":{
           |      "algorithm":"md5",
           |      "value":"a3c2f1e38701bd2c7b54ebd7b1cd0dbc"
           |    },
           |    "size":12345
           |  },
           |  "audit":{
           |    "correlationID":"testCorrelationId"
           |  }
           |}""".stripMargin
      ))

    // TODO: there is problem with test data which is not deterministic and is missing data. (APB-10869)
    ObjectStoreStubs
      .getRequestBody(fileName = fileName) shouldBe
      s"""00|ARR|SAS|20591125|163351
         |01|Entity|N|${application.agentApplication.applicationReference.value}|Alice Smith|(+44) 10794554342|user@test.com|LimitedLiabilityPartnership|1234567895|1234567890|123456789,123456789|123/AB12345,123/AB12345|HMRC|XAML00000123456|25-11-2059|evidence-reference-123|||||||||||
         |01|Individual|N||||||||123456789,123456789|123/AB12345,123/AB12345|||||${personReference1.value}|||Test Name|01-01-1980|AB123456C|1234567895|(+44) 10794554342|member@test.com|N|Y
         |01|Individual|N||||||||123456789,123456789|123/AB12345,123/AB12345|||||${personReference2.value}|||Test Name|01-01-1980|AB123456C|1234567895|(+44) 10794554342|member@test.com|N|Y
         |01|Individual|N||||||||123456789,123456789|123/AB12345,123/AB12345|||||${personReference3.value}|||Test Name|01-01-1980|AB123456C|1234567895|(+44) 10794554342|member@test.com|N|Y
         |99|4
         |"""
        .stripMargin

    val updatedApplication = repo.findByApplicationReference(application.agentApplication.applicationReference).futureValue.value
    updatedApplication.status shouldBe RiskingStatus.SubmittedForRisking
    updatedApplication.riskingFileId.isDefined shouldBe true

    val savedRiskingFile = riskingFileRepo.findById(updatedApplication.riskingFileId.value).futureValue.value
    savedRiskingFile.fineName shouldBe fileName
