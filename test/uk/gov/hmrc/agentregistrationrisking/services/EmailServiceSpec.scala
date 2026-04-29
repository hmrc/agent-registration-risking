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
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRiskingId
import uk.gov.hmrc.agentregistrationrisking.model.EmailInformation
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRiskingId
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.*
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.EmailStubs

class EmailServiceSpec
extends ISpec:

  val service: EmailService = app.injector.instanceOf[EmailService]
  val repo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  val individualRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  private val expectedRecipient: String = "user@test.com"
  private val expectedBusinessName: String = "Test Company Name"
  private val expectedAgentName: String = "Alice Smith"
  private val expectedApplicationRef: String = "ABC123456"
  private val expectedIndividualName: String = "Test Name"
  private val expectedIndividualRecipient: String = "member@test.com"
  private val expectedParameters: Map[String, String] = Map(
    "agentName" -> expectedAgentName,
    "applicationRef" -> expectedApplicationRef,
    "businessName" -> expectedBusinessName
  )
  private val expectedIndividualParameters: Map[String, String] = Map(
    "individualName" -> expectedIndividualName
  )

  private val applicantNonFixableFailureEmail = EmailInformation(
    to = Seq(expectedRecipient),
    templateId = EmailService.applicationNonFixableFailureTemplateId,
    parameters = Map(
      "agentName" -> expectedAgentName,
      "applicationRef" -> expectedApplicationRef
    )
  )

  private val individualNonFixableFailureEmail = EmailInformation(
    to = Seq(expectedIndividualRecipient),
    templateId = EmailService.individualNonFixableFailureTemplateId,
    parameters = expectedIndividualParameters
  )

  private def seedAppWithIndividual(
    application: ApplicationForRisking,
    individualFailures: Option[List[IndividualFailure]]
  ): IndividualForRisking =
    val base = tdAll.readyForSubmissionIndividual(application._id)
    val individual = base.copy(
      _id = IndividualForRiskingId(s"${application._id.value}-ind"),
      individualProvidedDetails = base.individualProvidedDetails.copy(
        personReference = PersonReference(s"${application._id.value}-pref")
      ),
      failures = individualFailures
    )
    repo.upsert(application).futureValue
    individualRepo.upsert(individual).futureValue
    individual

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

  "findAndSendNonFixableFailureEmails sends the applicant non-fixable failure email when the entity has non-fixable failures and individuals are approved" in:

    val application = llpApplicationForRisking.copy(
      _id = ApplicationForRiskingId("nf-1"),
      failures = Some(List(EntityFailure._7)),
      isSubscribed = false,
      isEmailSent = false
    )
    seedAppWithIndividual(application, individualFailures = Some(List.empty))

    EmailStubs.stubSendEmail(applicantNonFixableFailureEmail)

    service.findAndSendNonFixableFailureEmails().futureValue shouldBe (())

    EmailStubs.verifySendEmail(count = 1)
    repo.findApplicationsReadyForFailureEmailCheck().futureValue shouldBe empty

  "findAndSendNonFixableFailureEmails sends both applicant and individual failure emails when an individual is non-fixable" in:

    val application = llpApplicationForRisking.copy(
      _id = ApplicationForRiskingId("nf-2"),
      failures = Some(List.empty),
      isSubscribed = false,
      isEmailSent = false
    )
    seedAppWithIndividual(application, individualFailures = Some(List(IndividualFailure._6)))

    EmailStubs.stubSendEmail(applicantNonFixableFailureEmail)
    EmailStubs.stubSendEmail(individualNonFixableFailureEmail)

    service.findAndSendNonFixableFailureEmails().futureValue shouldBe (())

    EmailStubs.verifySendEmail(count = 2)
    repo.findApplicationsReadyForFailureEmailCheck().futureValue shouldBe empty

  "findAndSendNonFixableFailureEmails skips the individual email for sole trader applications to avoid duplicates" in:

    val soleTraderAgentApplication = tdAll.agentApplicationSoleTrader.afterAmlsComplete
    val application = llpApplicationForRisking.copy(
      _id = ApplicationForRiskingId("nf-sole-1"),
      agentApplication = soleTraderAgentApplication,
      failures = Some(List(EntityFailure._7)),
      isSubscribed = false,
      isEmailSent = false
    )
    seedAppWithIndividual(application, individualFailures = Some(List(IndividualFailure._6)))

    EmailStubs.stubSendEmail(applicantNonFixableFailureEmail)
    EmailStubs.stubSendEmail(individualNonFixableFailureEmail) // not expected to be hit

    service.findAndSendNonFixableFailureEmails().futureValue shouldBe (())

    EmailStubs.verifySendEmail(count = 1) // only the applicant email
    repo.findApplicationsReadyForFailureEmailCheck().futureValue shouldBe empty

  "findAndSendNonFixableFailureEmails does nothing when no candidates" in:

    EmailStubs.stubSendEmail(applicantNonFixableFailureEmail) // not expected to be hit

    service.findAndSendNonFixableFailureEmails().futureValue shouldBe (())

    EmailStubs.verifySendEmail(count = 0)

  "findAndSendNonFixableFailureEmails marks applications as emailed even when applicant send fails" in:

    val application = llpApplicationForRisking.copy(
      _id = ApplicationForRiskingId("nf-fail-applicant"),
      failures = Some(List(EntityFailure._7)),
      isSubscribed = false,
      isEmailSent = false
    )
    seedAppWithIndividual(application, individualFailures = Some(List.empty))

    EmailStubs.stubSendEmailFailure(applicantNonFixableFailureEmail)

    service.findAndSendNonFixableFailureEmails().futureValue shouldBe (())

    EmailStubs.verifySendEmail(count = 1)
    repo.findApplicationsReadyForFailureEmailCheck().futureValue shouldBe empty
