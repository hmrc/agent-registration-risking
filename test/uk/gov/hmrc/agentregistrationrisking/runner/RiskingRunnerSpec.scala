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
import uk.gov.hmrc.agentregistrationrisking.model.sdes.NotifySdesFile
import uk.gov.hmrc.agentregistrationrisking.model.sdes.NotifySdesFileReadyRequest
import uk.gov.hmrc.agentregistrationrisking.model.sdes.SdesInformationType
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.RiskingFileRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.ObjectStoreStubs
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.SdesProxyStubs

class RiskingRunnerSpec
extends ISpec:

  "build risking file" in:
    given request: Request[?] = tdAll.backendRequest
    val (riskingFileWithContent: RiskingFileWithContent, applicationReferences: Seq[ApplicationReference]) = riskingRunner.buildRiskingFile().futureValue

    riskingFileWithContent.riskingFile shouldBe RiskingFile(
      riskingFileName = fileName,
      uploadedAt = tdAll.instant
    )

    applicationReferences shouldBe List(
      tdAll.tdRiskingInstancesInStates.readyForSubmission.application.applicationReference,
      tdAll.tdRiskingInstancesInStates.readyForSubmission2.application.applicationReference
    )

    riskingFileWithContent.riskingFileContent `shouldBeLike` expectedFileContent

  "build risking file and sent to minerva" in:
    given request: Request[?] = tdAll.backendRequest

    ObjectStoreStubs.stubPutObject(
      fileName = fileName.value
    )
    SdesProxyStubs.stubSdesFileReady(tdAll.notifySdesFileReadyRequest)
    riskingRunner.run().futureValue

    ObjectStoreStubs.verifyPutObject(
      fileName = fileName.value
    )
    SdesProxyStubs.verifySdesFileReady()

    val riskingFileContent: String = ObjectStoreStubs.getRequestBody(fileName.value)
    riskingFileContent `shouldBeLike` expectedFileContent

    val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
    applicationForRiskingRepo.findReadyForSubmission().futureValue shouldBe List.empty withClue "no more records to submit at this stage"

  private val fileName: RiskingFileName = RiskingFileName("asa_risking_file_version1_0_4_20591125_163351.txt")
  private val expectedFileContent: String =
    """00|ARR|SAS|20591125|163351
      |01|Entity|N|APPREF_readyForSubmission|applicantname_readyForSubmission|01234567890|applicantemail@readyForSubmission.com|LimitedPartnership|utr_readyForSubmission|crn_readyForSubmission|vrn_readyForSubmission|payeref_readyForSubmission|amlscode_readyForSubmission|amlsregistrationnumber_readyForSubmission||https://admin.tax.service.gov.uk/agent-helpdesk/amls-evidence/amls_fileupload_refreadyForSubmission|||||||||||
      |01|Entity|N|APPREF_readyForSubmission2|applicantname_readyForSubmission2|01234567890|applicantemail@readyForSubmission2.com|LimitedCompany|utr_readyForSubmission2|crn_readyForSubmission2|vrn_readyForSubmission2|payeref_readyForSubmission2|amlscode_readyForSubmission2|amlsregistrationnumber_readyForSubmission2||https://admin.tax.service.gov.uk/agent-helpdesk/amls-evidence/amls_fileupload_refreadyForSubmission2|||||||||||
      |01|Individual|N||||||||vrn_readyForSubmission_01|payeref_readyForSubmission_01|||||PREF_readyForSubmission_01|||IndividualName_readyForSubmission_01|01-01-2008|AB123456C_readyForSubmission_01|1234567895_readyForSubmission_01|01234567-39|individual_email_readyForSubmission_01@test.com|N|Y
      |01|Individual|N||||||||vrn_readyForSubmission_02|payeref_readyForSubmission_02|||||PREF_readyForSubmission_02|||IndividualName_readyForSubmission_02|01-01-2002|AB123456C_readyForSubmission_02|1234567895_readyForSubmission_02|01234567-146|individual_email_readyForSubmission_02@test.com|N|Y
      |01|Individual|N||||||||vrn_readyForSubmission2_01|payeref_readyForSubmission2_01|||||PREF_readyForSubmission2_01|||IndividualName_readyForSubmission2_01|01-01-1986|AB123456C_readyForSubmission2_01|1234567895_readyForSubmission2_01|01234567-916|individual_email_readyForSubmission2_01@test.com|N|Y
      |01|Individual|N||||||||vrn_readyForSubmission2_02|payeref_readyForSubmission2_02|||||PREF_readyForSubmission2_02|||IndividualName_readyForSubmission2_02|01-01-1985|AB123456C_readyForSubmission2_02|1234567895_readyForSubmission2_02|01234567-206|individual_email_readyForSubmission2_02@test.com|N|N
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
    tdAll
      .tdRiskingInstancesInStates
      .all
      .foreach: td =>
        applicationForRiskingRepo.upsert(td.application).futureValue
        individualForRiskingRepo.upsert(td.individual1).futureValue
        individualForRiskingRepo.upsert(td.individual2).futureValue

  extension (s: String)
    def getLines: Array[String] = s.split("\\R").filter(_.nonEmpty)

  extension (actual: RiskingFileWithContent.RiskingFileContent)
    def shouldBeLike(expected: RiskingFileWithContent.RiskingFileContent): Unit =

      actual shouldBe expected

      val actualLines = actual.getLines
      val expectedLines = expected.getLines
      actualLines.head shouldBe expectedLines.head
      actualLines.last shouldBe expectedLines.last

      actualLines.length shouldBe expectedLines.length

      actualLines
        .sorted.zip(expectedLines.sorted)
        .foreach:
          case (actualLine, expectedLine) => actualLine shouldBe expectedLine
