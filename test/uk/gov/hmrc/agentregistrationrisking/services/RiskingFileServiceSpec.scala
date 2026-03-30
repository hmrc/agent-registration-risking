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

import uk.gov.hmrc.agentregistration.shared.risking.ApplicationForRiskingStatus
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.risking.PersonReference
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.randomId

class RiskingFileServiceSpec
extends ISpec:

  val repo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  val service: RiskingFileService = app.injector.instanceOf[RiskingFileService]
  val personReference1: PersonReference = PersonReference(randomId)
  val personReference2: PersonReference = PersonReference(randomId)
  val personReference3: PersonReference = PersonReference(randomId)

  "buildRiskingFile retrieves all applications ready for risking and creates risking file in correct format" in:

    val readyForSubmissionUpsert = repo.upsert(tdAll.llpApplicationForRisking.copy(individuals =
      List(
        tdAll.readyForSubmissionIndividual(Some(personReference1)),
        tdAll.readyForSubmissionIndividual(Some(personReference2)),
        tdAll.readyForSubmissionIndividual(Some(personReference3))
      )
    ))
    readyForSubmissionUpsert.futureValue

    val result: String = service.buildRiskingFile.futureValue
    val recordCount = result.substring(result.length - 1).toInt
    recordCount shouldBe 4
    val pipeCount = result.count(_ == '|')
    pipeCount shouldBe 109

  "buildRiskingFile ignores any applications without a status of readyForSubmission" in:

    val readyForSubmissionUpsert = repo.upsert(tdAll.llpApplicationForRisking.copy(individuals =
      List(
        tdAll.readyForSubmissionIndividual(Some(personReference1)),
        tdAll.readyForSubmissionIndividual(Some(personReference2)),
        tdAll.readyForSubmissionIndividual(Some(personReference3))
      )
    ))
    readyForSubmissionUpsert.futureValue
    val submittedUpsert = repo.upsert(tdAll.llpApplicationForRisking.copy(
      applicationReference = ApplicationReference(randomId),
      status = ApplicationForRiskingStatus.SubmittedForRisking,
      individuals = List(
        tdAll.readyForSubmissionIndividual(Some(PersonReference("personReference4"))),
        tdAll.readyForSubmissionIndividual(Some(PersonReference("personReference5"))),
        tdAll.readyForSubmissionIndividual(Some(PersonReference("personReference6")))
      )
    ))
    submittedUpsert.futureValue

    val result: String = service.buildRiskingFile.futureValue
    val recordCount = result.substring(result.length - 1).toInt
    recordCount shouldBe 4
    val pipeCount = result.count(_ == '|')
    pipeCount shouldBe 109

  "getApplicationsReadyForRisking retrieves all applications ready for risking" in:

    val readyForSubmissionApplication = tdAll.llpApplicationForRisking.copy(
      individuals = List(
        tdAll.readyForSubmissionIndividual(Some(personReference1)),
        tdAll.readyForSubmissionIndividual(Some(personReference2)),
        tdAll.readyForSubmissionIndividual(Some(personReference3))
      )
    )

    val submittedApplication = tdAll.llpApplicationForRisking.copy(
      applicationReference = ApplicationReference(randomId),
      status = ApplicationForRiskingStatus.SubmittedForRisking,
      individuals = List(
        tdAll.readyForSubmissionIndividual(Some(PersonReference("personReference4"))),
        tdAll.readyForSubmissionIndividual(Some(PersonReference("personReference5"))),
        tdAll.readyForSubmissionIndividual(Some(PersonReference("personReference6")))
      )
    )

    repo.upsert(readyForSubmissionApplication).futureValue
    repo.upsert(submittedApplication).futureValue

    val result: Seq[ApplicationForRisking] = service.getApplicationsReadyForRisking.futureValue

    result.length shouldBe 1
    result.headOption.map(_.status) shouldBe Some(ApplicationForRiskingStatus.ReadyForSubmission)
    result.headOption.map(_.applicationReference) shouldBe Some(readyForSubmissionApplication.applicationReference)

  "buildRiskingFileFrom creates risking file in correct format from supplied applications" in:

    val applicationsReadyForRisking = Seq(
      tdAll.llpApplicationForRisking.copy(individuals =
        List(
          tdAll.readyForSubmissionIndividual(Some(personReference1)),
          tdAll.readyForSubmissionIndividual(Some(personReference2)),
          tdAll.readyForSubmissionIndividual(Some(personReference3))
        )
      )
    )

    val result: String = service.buildRiskingFileFrom(applicationsReadyForRisking)
    result shouldBe
      s"""00|ARR|SAS|20591125|163351
         |01|Entity|N|${tdAll.llpApplicationForRisking.applicationReference.value}|Alice Smith|(+44) 10794554342|user@test.com|LimitedLiabilityPartnership|1234567895|1234567890|123456789,123456789|123/AB12345,123/AB12345|HMRC|XAML00000123456|25-11-2059|evidence-reference-123|||||||||||
         |01|Individual|N||||||||123456789,123456789|123/AB12345,123/AB12345|||||${personReference1.value}|||Test Name|01-01-1980|AB123456C|1234567895|(+44) 10794554342|member@test.com|Y|Y
         |01|Individual|N||||||||123456789,123456789|123/AB12345,123/AB12345|||||${personReference2.value}|||Test Name|01-01-1980|AB123456C|1234567895|(+44) 10794554342|member@test.com|Y|Y
         |01|Individual|N||||||||123456789,123456789|123/AB12345,123/AB12345|||||${personReference3.value}|||Test Name|01-01-1980|AB123456C|1234567895|(+44) 10794554342|member@test.com|Y|Y
         |99|4"""
        .stripMargin
