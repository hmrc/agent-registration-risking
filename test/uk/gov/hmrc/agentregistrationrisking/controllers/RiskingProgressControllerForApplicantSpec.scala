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

import play.api.mvc.Call
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.risking.RiskingProgress
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.AuthStubs

import java.net.URL

class RiskingProgressControllerForApplicantSpec
extends ControllerSpec:

  val tdRisking: TdRisking = tdAll.tdRisking
  val applicationReference: ApplicationReference = tdRisking.agentApplication.applicationReference

  val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  val path: String = s"/agent-registration-risking/risking-progress/for-applicant"
  def riskingProgressForApplicantUrl(agentApplicationReference: ApplicationReference): URL = url"${baseUrl + path}/${agentApplicationReference.value}"

  def primeDbWithBackgroundData(): Unit =
    applicationForRiskingRepo.collection.drop()
    individualForRiskingRepo.collection.drop()
    applicationForRiskingRepo.upsert(tdAll.tdRisking2.tdApplicationForRisking.readyForSubmission).futureValue
    individualForRiskingRepo.upsert(tdAll.tdRisking2.tdIndividualsForRisking.tdIndividualForRisking1.readyForSubmission).futureValue
    individualForRiskingRepo.upsert(tdAll.tdRisking2.tdIndividualsForRisking.tdIndividualForRisking2.readyForSubmission).futureValue
    applicationForRiskingRepo.upsert(tdAll.tdRisking3.tdApplicationForRisking.submittedForRisking).futureValue
    individualForRiskingRepo.upsert(tdAll.tdRisking3.tdIndividualsForRisking.tdIndividualForRisking1.submittedForRisking).futureValue
    individualForRiskingRepo.upsert(tdAll.tdRisking3.tdIndividualsForRisking.tdIndividualForRisking2.submittedForRisking).futureValue
    ()

  "routes should have correct paths and methods" in:
    val call: Call = routes.RiskingProgressController.getRiskingProgressForApplicant(applicationReference)
    call shouldBe Call(
      method = "GET",
      url = s"$path/${applicationReference.value}"
    )

  "returns NO_CONTENT if there is no underlying records" in:
    given Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthorise()

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

  final case class TestCase(
    description: String,
    application: ApplicationForRisking,
    individuals: Seq[IndividualForRisking],
    expectedRiskingProgress: RiskingProgress
  )

  val testCases: Seq[TestCase] = List(
    TestCase(
      description = "application is readyForSubmission",
      application = tdRisking.tdApplicationForRisking.readyForSubmission,
      individuals = Seq(
        tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.readyForSubmission,
        tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.readyForSubmission
      ),
      expectedRiskingProgress = RiskingProgress.ReadyForSubmission
    ),
    TestCase(
      description = "application is submittedForRisking",
      application = tdRisking.tdApplicationForRisking.submittedForRisking,
      individuals = Seq(
        tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.submittedForRisking,
        tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.submittedForRisking
      ),
      expectedRiskingProgress = RiskingProgress.SubmittedForRisking
    ),
    TestCase(
      description = "partial risking results: application:approved, individual1:submittedForRisking, individual2:submittedForRisking",
      application = tdRisking.tdApplicationForRisking.receivedRiskingResults.approved,
      individuals = Seq(
        tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.submittedForRisking,
        tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.submittedForRisking
      ),
      expectedRiskingProgress = RiskingProgress.SubmittedForRisking
    ),
    TestCase(
      description = "partial risking results: application:approved, individual1:approved, individual2: submittedForRisking",
      application = tdRisking.tdApplicationForRisking.receivedRiskingResults.approved,
      individuals = Seq(
        tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.approved,
        tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.submittedForRisking
      ),
      expectedRiskingProgress = RiskingProgress.SubmittedForRisking
    ),
    TestCase(
      description = "all approved: application:approved, individual1:approved, individual2:approved",
      application = tdRisking.tdApplicationForRisking.receivedRiskingResults.approved,
      individuals = Seq(
        tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.approved,
        tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.approved
      ),
      expectedRiskingProgress = RiskingProgress.Approved
    )
  )

  primeDbWithBackgroundData()

  testCases.foreach: tc =>
    tc.description in:
      applicationForRiskingRepo.upsert(tc.application).futureValue
      tc.individuals.foreach(individualForRiskingRepo.upsert(_).futureValue)
      given Request[?] = tdAll.backendRequest
      AuthStubs.stubAuthorise()

      val applicationReference: ApplicationReference = tc.application.applicationReference
      val riskingStatusForApplicantResponse: HttpResponse =
        httpClient
          .get(riskingProgressForApplicantUrl(applicationReference))
          .execute[HttpResponse]
          .futureValue

      riskingStatusForApplicantResponse.status shouldBe Status.OK
      val riskingStatusForApplicant: RiskingProgress = riskingStatusForApplicantResponse.json.as[RiskingProgress]
      riskingStatusForApplicant shouldBe tc.expectedRiskingProgress
      AuthStubs.verifyAuthorise()

//  "find application with individuals returns Ok with individuals in response" in:
//    given Request[?] = tdAll.backendRequest
//    AuthStubs.stubAuthorise()
//
//    val application = tdAll.llpApplicationForRisking.copy(_id = ApplicationForRiskingId("test-app-with-individuals"))
//    applicationForRiskingRepo.upsert(application).futureValue
//
//    val individual = tdAll.readyForSubmissionIndividual(application._id)
//    individualForRiskingRepo.upsert(individual).futureValue
//
//    val response =
//      httpClient
//        .get(url"$baseUrl/agent-registration-risking/application/${application.agentApplication.applicationReference.value}")
//        .execute[HttpResponse]
//        .futureValue
//    response.status shouldBe Status.OK
//    val parsedResponse = response.json.as[RiskingProgress]
//    parsedResponse.applicationReference shouldBe application.agentApplication.applicationReference
//    parsedResponse.individuals.size shouldBe 1
//    parsedResponse.individuals.headOption.value.personReference shouldBe tdAll.personReference
//    parsedResponse.individuals.headOption.value.failures shouldBe None
//    AuthStubs.verifyAuthorise()
