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

import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRiskingId
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRiskingId
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileName
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.randomId

class RiskingFileServiceSpec
extends ISpec:

  val repo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  val individualRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]
  val service: RiskingFileService = app.injector.instanceOf[RiskingFileService]
  val personReference1: PersonReference = PersonReference(randomId())
  val personReference2: PersonReference = PersonReference(randomId())
  val personReference3: PersonReference = PersonReference(randomId())

  "getApplicationsReadyForRiskingWithIndividuals retrieves only ready applications with their individuals" in:

    val readyApplication = tdAll.llpApplicationForRisking.copy(
      _id = ApplicationForRiskingId("ready-app")
    )
    val readyIndividual = tdAll.readyForSubmissionIndividual(readyApplication._id).copy(
      _id = IndividualForRiskingId("ready-ind")
    )

    val submittedApplication = tdAll.llpApplicationForRisking.copy(
      _id = ApplicationForRiskingId("submitted-app"),
      riskingFileName = Some(RiskingFileName("some-file-id"))
    )

    repo.upsert(readyApplication).futureValue
    repo.upsert(submittedApplication).futureValue
    individualRepo.upsert(readyIndividual).futureValue

    val result = service.getApplicationsReadyForRiskingWithIndividuals.futureValue
    result.size shouldBe 1
    result.headOption.value.application._id shouldBe readyApplication._id
    result.headOption.value.individuals.size shouldBe 1
    result.headOption.value.individuals.headOption.value._id shouldBe readyIndividual._id

  "buildRiskingFileFrom creates risking file in correct format from supplied applications" in:

    val application = tdAll.llpApplicationForRisking

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

    import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals

    val result: String = service.buildRiskingFileFrom(Seq(
      ApplicationWithIndividuals(
        application,
        Seq(
          individual1,
          individual2,
          individual3
        )
      )
    ))

    result shouldBe
      s"""00|ARR|SAS|20591125|163351
         |01|Entity|N|${tdAll.llpApplicationForRisking.agentApplication.applicationReference.value}|Alice Smith|(+44) 10794554342|user@test.com|LimitedLiabilityPartnership|1234567895|1234567890|123456789,123456789|123/AB12345,123/AB12345|HMRC|XAML00000123456||evidence-reference-123|||||||||||
         |01|Individual|N||||||||123456789,123456789|123/AB12345,123/AB12345|||||${personReference1.value}|||Test Name|01-01-1980|AB123456C|1234567895|(+44) 10794554342|member@test.com|N|Y
         |01|Individual|N||||||||123456789,123456789|123/AB12345,123/AB12345|||||${personReference2.value}|||Test Name|01-01-1980|AB123456C|1234567895|(+44) 10794554342|member@test.com|N|Y
         |01|Individual|N||||||||123456789,123456789|123/AB12345,123/AB12345|||||${personReference3.value}|||Test Name|01-01-1980|AB123456C|1234567895|(+44) 10794554342|member@test.com|N|Y
         |99|4
         |"""
        .stripMargin
