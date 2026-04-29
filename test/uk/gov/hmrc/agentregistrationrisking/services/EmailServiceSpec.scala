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

import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRiskingId
import uk.gov.hmrc.agentregistrationrisking.model.EmailInformation
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.*
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.EmailStubs

class EmailServiceSpec
extends ISpec:

  val service: EmailService = app.injector.instanceOf[EmailService]
  val repo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]

  private val expectedRecipient: String = "user@test.com"
  private val expectedBusinessName: String = "Test Company Name"
  private val expectedAgentName: String = "Alice Smith"
  private val expectedApplicationRef: String = "ABC123456"
  private val expectedParameters: Map[String, String] = Map(
    "agentName" -> expectedAgentName,
    "applicationRef" -> expectedApplicationRef,
    "businessName" -> expectedBusinessName
  )

  "sendEmail forwards the EmailInformation to the email service" in:

    given RequestHeader = FakeRequest()

    EmailStubs.stubSendEmail(testEmailInformation)

    service.sendEmail(testEmailInformation).futureValue shouldBe (())

    EmailStubs.verifySendEmail()

  "sendRegisteredEmail sends the agent_registration_success template to the agent's verified email address" in:

    given RequestHeader = FakeRequest()

    val expectedEmail = EmailInformation(
      to = Seq(expectedRecipient),
      templateId = EmailService.registrationSuccessTemplateId,
      parameters = expectedParameters
    )
    EmailStubs.stubSendEmail(expectedEmail)

    service.sendRegisteredEmail(llpApplicationForRisking).futureValue shouldBe (())

    EmailStubs.verifySendEmail()

  "findAndSendRegisteredEmail sends a success email to every subscribed-ready-for-email application and marks them as emailed" in:

    val readyApplication = llpApplicationForRisking.copy(
      _id = ApplicationForRiskingId("send-and-mark-1"),
      isSubscribed = true,
      isEmailSent = false
    )
    repo.upsert(readyApplication).futureValue

    val expectedEmail = EmailInformation(
      to = Seq(expectedRecipient),
      templateId = EmailService.registrationSuccessTemplateId,
      parameters = expectedParameters
    )
    EmailStubs.stubSendEmail(expectedEmail)

    service.findAndSendRegisteredEmail().futureValue shouldBe (())

    EmailStubs.verifySendEmail()
    repo.findSubscribedReadyForSuccessEmail().futureValue shouldBe empty

  "findAndSendRegisteredEmail does nothing when no applications are ready" in:

    EmailStubs.stubSendEmail(testEmailInformation) // not expected to be hit

    service.findAndSendRegisteredEmail().futureValue shouldBe (())

    EmailStubs.verifySendEmail(count = 0)

  "findAndSendRegisteredEmail marks applications as emailed even when the send fails to avoid unbounded retries" in:

    val readyApplication = llpApplicationForRisking.copy(
      _id = ApplicationForRiskingId("send-failing-1"),
      isSubscribed = true,
      isEmailSent = false
    )
    repo.upsert(readyApplication).futureValue

    val expectedEmail = EmailInformation(
      to = Seq(expectedRecipient),
      templateId = EmailService.registrationSuccessTemplateId,
      parameters = expectedParameters
    )
    EmailStubs.stubSendEmailFailure(expectedEmail)

    service.findAndSendRegisteredEmail().futureValue shouldBe (())

    EmailStubs.verifySendEmail()
    repo.findSubscribedReadyForSuccessEmail().futureValue shouldBe empty
