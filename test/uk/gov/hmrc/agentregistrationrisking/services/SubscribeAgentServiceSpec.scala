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

import play.api.mvc.AnyContent
import play.api.mvc.Request
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationForRiskingStatus
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.risking.PersonReference
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.hip.Arn
import uk.gov.hmrc.agentregistrationrisking.model.hip.SubscribeAgentRequest
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.randomId
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.HipStubs

class SubscribeAgentServiceSpec
extends ISpec:

  val repo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  val service: SubscribeAgentService = app.injector.instanceOf[SubscribeAgentService]

  "subscribeAgent call should throw an error when the application status is not Approved" in:

    val notApprovedApplication: ApplicationForRisking = tdAll.llpApplicationForRisking.copy(
      applicationReference = ApplicationReference(randomId),
      status = ApplicationForRiskingStatus.ReadyForSubmission,
      individuals = List(
        tdAll.approvedIndividual(PersonReference("personReference"))
      )
    )

    repo.upsert(notApprovedApplication).futureValue
    given request: Request[AnyContent] = TdAll.tdAll.fakeBackendRequest

    val expectedException =
      the[IllegalArgumentException] thrownBy
        service.subscribeAgent(notApprovedApplication)

    expectedException.getMessage should include("Application must have the Approved status")

  "subscribeAgent call should return an Arn and update the status to SubscribedAndEnrolled when the application status is Approved" in :
    
    val expectedArn = Arn("ARN1234567")
    val approvedApplication: ApplicationForRisking = tdAll.llpApplicationForRisking.copy(
      applicationReference = ApplicationReference(randomId),
      status = ApplicationForRiskingStatus.Approved,
      individuals = List(
        tdAll.approvedIndividual(PersonReference("personReference"))
      )
    )

    repo.upsert(approvedApplication).futureValue
    
    HipStubs.stubSubscribeAgent(
      safeId = approvedApplication.entitySafeId,
      subscribeAgentRequest = SubscribeAgentRequest(
        name = approvedApplication.agentDetails.businessName.getAgentBusinessName,
        addr1 = approvedApplication.agentDetails.getAgentCorrespondenceAddress.addressLine1,
        addr2 = approvedApplication.agentDetails.getAgentCorrespondenceAddress.addressLine2.getOrElse(""),
        addr3 = approvedApplication.agentDetails.getAgentCorrespondenceAddress.addressLine3,
        addr4 = approvedApplication.agentDetails.getAgentCorrespondenceAddress.addressLine4,
        postcode = approvedApplication.agentDetails.getAgentCorrespondenceAddress.postalCode,
        country = approvedApplication.agentDetails.getAgentCorrespondenceAddress.countryCode,
        phone = Some(approvedApplication.agentDetails.getTelephoneNumber.agentTelephoneNumber),
        email = approvedApplication.agentDetails.getAgentEmailAddress.getEmailAddress,
        supervisoryBody = Some(approvedApplication.amlSupervisoryBody.value),
        membershipNumber = Some(approvedApplication.amlRegNumber.value),
        evidenceObjectReference = None,
        updateDetailsStatus = "ACCEPTED",
        amlSupervisionUpdateStatus = "ACCEPTED",
        directorPartnerUpdateStatus = "ACCEPTED",
        acceptNewTermsStatus = "ACCEPTED",
        reriskStatus = "ACCEPTED"
      ),
      arn = expectedArn
    )
    given request: Request[AnyContent] = TdAll.tdAll.fakeBackendRequest
