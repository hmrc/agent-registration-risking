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

package uk.gov.hmrc.agentregistrationrisking.controllers.smu

import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.smu.SmuIndividualResponse
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging
import play.api.mvc.Call

import java.net.URL

class SmuViewerControllerSpec
extends ControllerSpec
with RequestAwareLogging:

  def path(personReference: PersonReference) = s"/agent-registration-risking/smu-viewer/individual/by-person-reference/${personReference.value}"
  def url(personReference: PersonReference): URL = url"${baseUrl + path(personReference)}"

  "routes should have correct paths and methods" in:
    val personReference: PersonReference = tdAll.tdRiskingInstancesInStates.submittedForRisking.individual1.personReference
    val call: Call = routes.SmuViewerController.findIndividualByPersonReference(personReference)
    call shouldBe Call(
      method = "GET",
      url = path(personReference)
    )

  "return NoContent if there is no underlying records" in:
    given Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthorise()

    val response: HttpResponse =
      httpClient
        .get(url(PersonReference("NOSUCH_REF")))
        .execute[HttpResponse]
        .futureValue

    response.status shouldBe Status.NO_CONTENT
    response.body shouldBe ""

  "find individual by person reference returns Ok and SmuIndividualResponse as Json body" in:
    given Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthorise()

    val individual: IndividualForRisking = tdAll.tdRiskingInstancesInStates.submittedForRisking.individual1
    val application: ApplicationForRisking = tdAll.tdRiskingInstancesInStates.submittedForRisking.application

    val response: HttpResponse =
      httpClient
        .get(url(individual.personReference))
        .execute[HttpResponse]
        .futureValue

    response.status shouldBe Status.OK
    val json = Json.parse(
      // language=JSON
      s"""{
         |  "individual": {
         |    "personReference": "PREF_submittedForRisking_01",
         |    "resubmission": false,
         |    "passedIdentityVerification": true,
         |    "detailsProvidedByApplicant": false,
         |    "individualName": "IndividualName_submittedForRisking_01",
         |    "individualDateOfBirth": "1998-01-01",
         |    "individualNino": "AB123456C_submittedForRisking_01",
         |    "individualSaUtr": "1234567895_submittedForRisking_01",
         |    "payeRefs": [
         |      "payeref_submittedForRisking_01"
         |    ],
         |    "vrns": [
         |      "vrn_submittedForRisking_01"
         |    ],
         |    "telephoneNumber": "01234567-201",
         |    "emailAddress": "individual_email_submittedForRisking_01@test.com"
         |  },
         |  "entity": {
         |    "applicationReference": "APPREF_submittedForRisking",
         |    "resubmission": false,
         |    "applicantName": "applicantname_submittedForRisking",
         |    "businessType": "ScottishPartnership",
         |    "utr": "utr_submittedForRisking",
         |    "payeRefs": [
         |      "payeref_submittedForRisking"
         |    ],
         |    "vrns": [
         |      "vrn_submittedForRisking"
         |    ],
         |    "crn": "crn_submittedForRisking",
         |    "amlsSupervisoryBody": "amlscode_submittedForRisking",
         |    "amlsRegNumber": "amlsregistrationnumber_submittedForRisking",
         |    "amlsEvidenceReferenceId": "amls_fileupload_refsubmittedForRisking",
         |    "applicantPhone": "01234567890",
         |    "applicantEmail": "applicantemail@submittedForRisking.com"
         |  }
         |}""".stripMargin
    )

    val smuViewerIndividualResponse: SmuIndividualResponse = json.as[SmuIndividualResponse]
    response.json.as[SmuIndividualResponse] shouldBe smuViewerIndividualResponse

  override def beforeEach(): Unit =
    super.beforeEach()
    primeDb()

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
