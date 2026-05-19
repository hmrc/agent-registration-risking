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

package uk.gov.hmrc.agentregistrationrisking.controllers

import org.mongodb.scala.SingleObservableFuture
import play.api.mvc.Call
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.risking.RiskingProgress
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.AuthStubs

import java.net.URL

class RiskingProgressControllerForApplicantSpec
extends ControllerSpec:

  val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  val pathForApplicant: String = s"/agent-registration-risking/risking-progress/for-applicant"
  val pathForIndividual: String = s"/agent-registration-risking/risking-progress/for-individual"

  def riskingProgressForApplicantUrl(agentApplicationReference: ApplicationReference): URL =
    url"${baseUrl + pathForApplicant}/${agentApplicationReference.value}"
  def riskingProgressForIndividualUrl(personRerence: PersonReference): URL = url"${baseUrl + pathForApplicant}/${personRerence.value}"

  "routes should have correct paths and methods" in:
    val applicationReference: ApplicationReference = ApplicationReference("APPREF_123")
    val call: Call = routes.RiskingProgressController.getRiskingProgressForApplicant(applicationReference)
    call shouldBe Call(
      method = "GET",
      url = s"$pathForApplicant/${applicationReference.value}"
    )

  "returns NO_CONTENT if there is no underlying records" in:
    given Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthorise()
    val applicationReference: ApplicationReference = ApplicationReference("APPREF_123")

    applicationForRiskingRepo.findById(applicationReference).futureValue shouldBe None withClue " no prior records in mongo for this application"
    individualForRiskingRepo.findByApplicationReference(
      applicationReference
    ).futureValue shouldBe Seq.empty withClue " no prior records in mongo for this application"

    val response: HttpResponse =
      httpClient
        .get(riskingProgressForApplicantUrl(applicationReference))
        .execute[HttpResponse]
        .futureValue

    response.status shouldBe Status.NO_CONTENT
    response.body shouldBe ""
    AuthStubs.verifyAuthorise()

  tdAll
    .tdRiskingInstancesInStates
    .all
    .foreach: (td: TdApplicationWithIndividuals) =>
      s"return correct riskingProgress - $td" in:
        AuthStubs.stubAuthorise()
        given Request[?] = tdAll.backendRequest
        val applicationReference: ApplicationReference = td.application.applicationReference
        val riskingStatusForApplicantResponse: HttpResponse =
          httpClient
            .get(riskingProgressForApplicantUrl(applicationReference))
            .execute[HttpResponse]
            .futureValue
        riskingStatusForApplicantResponse.status shouldBe Status.OK
        val riskingStatusForApplicant: RiskingProgress = riskingStatusForApplicantResponse.json.as[RiskingProgress]
        riskingStatusForApplicant shouldBe td.riskingProgressForApplicant
        AuthStubs.verifyAuthorise()

  override protected def beforeAll(): Unit =
    super.beforeAll()

    def primeDbWithBackgroundData(): Unit =
      applicationForRiskingRepo.collection.drop().toFuture.futureValue
      individualForRiskingRepo.collection.drop().toFuture.futureValue
      tdAll.tdRiskingInstancesInStates.all.foreach: td =>
        applicationForRiskingRepo.upsert(td.application).futureValue
        td.individuals.foreach(individualForRiskingRepo.upsert(_).futureValue)

    primeDbWithBackgroundData()
