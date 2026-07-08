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

import org.mongodb.scala.SingleObservableFuture
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.EmailTemplateId
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.SendEmailRequest
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdIndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.EmailStubs

class EmailServiceForApprovedApplicationsSpec
extends ISpec:

  private val emailServiceForApprovedApplications: EmailServiceForApprovedApplications = app.injector.instanceOf[EmailServiceForApprovedApplications]
  private val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  private val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  private given RequestHeader = tdAll.fakeBackendRequest

  override def beforeEach(): Unit =
    super.beforeEach()
    applicationForRiskingRepo.collection.drop().toFuture.futureValue
    individualForRiskingRepo.collection.drop().toFuture.futureValue
    ()

  private def expectedSuccessEmail(application: ApplicationForRisking): SendEmailRequest =
    val data = application.applicationData
    SendEmailRequest(
      to = Seq(data.agentDetails.agentEmailAddress),
      templateId = EmailTemplateId.RegistrationSuccess,
      parameters = Map(
        "agentName" -> data.applicantContactDetails.applicantName.value,
        "applicationRef" -> data.applicationReference.value,
        "businessName" -> data.agentDetails.businessName.getAgentBusinessName
      )
    )

  private val tdRisking: TdRisking = TdRisking.make("EmailServiceForApprovedApplicationsSpec")
  private val tdApplicationForRisking: TdApplicationForRisking = tdRisking.tdApplicationForRisking

  private val individual1: IndividualForRisking =
    tdRisking
      .tdIndividualsForRisking
      .tdIndividualForRisking1
      .receivedRiskingResults
      .approved

  private val individual2: IndividualForRisking =
    tdRisking
      .tdIndividualsForRisking
      .tdIndividualForRisking2
      .receivedRiskingResults
      .approved

  private def prime(application: ApplicationForRisking): Unit =
    applicationForRiskingRepo.upsert(application).futureValue
    individualForRiskingRepo.upsert(individual1).futureValue
    individualForRiskingRepo.upsert(individual2).futureValue

  "processEmails" - {

    "sends the success email and atomically flips isEmailSent + emailsProcessed + emailSentAt when the application is Approved AND subscribed AND not yet emailed" in:
      val application: ApplicationForRisking = tdApplicationForRisking.receivedRiskingResults.approvedAfterSubscribed

      EmailStubs.stubSendEmail(expectedSuccessEmail(application))
      prime(application)

      emailServiceForApprovedApplications.processEmails().futureValue

      EmailStubs.verifySendEmail(count = 1)
      val persistedApp: ApplicationForRisking = applicationForRiskingRepo.findById(application.applicationReference).futureValue.value
      persistedApp.isEmailSent shouldBe true
      persistedApp.overallStatus.emailsProcessed shouldBe true
      persistedApp.overallStatus.emailsSentAt shouldBe defined

    "does NOT send the success email when the application is Approved but NOT yet subscribed — subscription must complete first" in:
      val application: ApplicationForRisking = tdApplicationForRisking.receivedRiskingResults.approvedAfterOutcome

      prime(application)

      emailServiceForApprovedApplications.processEmails().futureValue

      EmailStubs.verifySendEmail(count = 0)
      val persistedApp: ApplicationForRisking = applicationForRiskingRepo.findById(application.applicationReference).futureValue.value
      persistedApp.isEmailSent shouldBe false
      persistedApp.overallStatus.emailsProcessed shouldBe false
      persistedApp.overallStatus.emailsSentAt shouldBe None

    "does NOT send the success email a second time when the application already had its email sent — idempotent under re-run of the scheduler" in:
      val application: ApplicationForRisking = tdApplicationForRisking.receivedRiskingResults.approvedAfterEmailSent

      prime(application)

      emailServiceForApprovedApplications.processEmails().futureValue

      EmailStubs.verifySendEmail(count = 0)
      val persistedApp: ApplicationForRisking = applicationForRiskingRepo.findById(application.applicationReference).futureValue.value
      persistedApp.isEmailSent shouldBe true withClue "state must be unchanged from the primed value"
      persistedApp.overallStatus.emailsProcessed shouldBe true
      persistedApp.overallStatus.emailsSentAt shouldBe defined
  }
