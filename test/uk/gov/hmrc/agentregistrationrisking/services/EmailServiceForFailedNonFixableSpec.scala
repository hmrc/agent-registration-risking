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

import com.softwaremill.quicklens.modify
import org.mongodb.scala.SingleObservableFuture
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.EmailTemplateId
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.SendEmailRequest
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.EmailStubs

class EmailServiceForFailedNonFixableSpec
extends ISpec:

  private val emailService: EmailServiceForFailedNonFixable = app.injector.instanceOf[EmailServiceForFailedNonFixable]
  private val applicationRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  private val individualRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  private given RequestHeader = tdAll.fakeBackendRequest

  override def beforeEach(): Unit =
    super.beforeEach()
    applicationRepo.collection.drop().toFuture.futureValue
    individualRepo.collection.drop().toFuture.futureValue
    ()

  private case class TestCase(
    description: String,
    application: ApplicationForRisking,
    individuals: Seq[IndividualForRisking],
    expectedEmails: Seq[SendEmailRequest]
  )

  private def expectedApplicantEmail(application: ApplicationForRisking): SendEmailRequest =
    val data = application.applicationData
    SendEmailRequest(
      to = Seq(data.agentDetails.agentEmailAddress),
      templateId = EmailTemplateId.ApplicationNonFixableFailure,
      parameters = Map(
        "agentName" -> data.applicantContactDetails.applicantName.value,
        "applicationRef" -> data.applicationReference.value
      )
    )

  private def expectedIndividualEmail(individual: IndividualForRisking): SendEmailRequest = SendEmailRequest(
    to = Seq(individual.individualData.emailAddress),
    templateId = EmailTemplateId.IndividualNonFixableFailure,
    parameters = Map(
      "individualName" -> individual.individualData.individualName.value
    )
  )

  private val tdRisking = TdRisking.make("EmailServiceForFailedNonFixableSpec")
  private val tdApp = tdRisking.tdApplicationForRisking.receivedRiskingResults
  private val tdInd1 = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults
  private val tdInd2 = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults
  private val tdInd3 = tdRisking.tdIndividualsForRisking.tdIndividualForRisking3.receivedRiskingResults

  private val soleTraderApp = tdApp.failedNonFixableAfterOutcome
    .modify(_.applicationData.businessType)
    .setTo(BusinessType.SoleTrader)
  private val soleTraderInd1 = tdInd1.failedNonFixable
    .modify(_.individualData.emailAddress)
    .setTo(soleTraderApp.applicationData.applicantContactDetails.applicantEmailAddress)

  "processEmails" - {

    List(
      TestCase(
        description = "sends 1 applicant email and 1 individual email when only 1 of 3 individuals has a NonFixable failure",
        application = tdApp.failedNonFixableAfterOutcome,
        individuals = Seq(
          tdInd1.failedNonFixable,
          tdInd2.approved,
          tdInd3.approved
        ),
        expectedEmails = Seq(
          expectedApplicantEmail(tdApp.failedNonFixableAfterOutcome),
          expectedIndividualEmail(tdInd1.failedNonFixable)
        )
      ),
      TestCase(
        description = "sends 1 applicant email and 2 individual emails when 2 of 3 individuals have a NonFixable failure",
        application = tdApp.failedNonFixableAfterOutcome,
        individuals = Seq(
          tdInd1.failedNonFixable,
          tdInd2.failedNonFixable,
          tdInd3.approved
        ),
        expectedEmails = Seq(
          expectedApplicantEmail(tdApp.failedNonFixableAfterOutcome),
          expectedIndividualEmail(tdInd1.failedNonFixable),
          expectedIndividualEmail(tdInd2.failedNonFixable)
        )
      ),
      TestCase(
        description = "sends only the applicant email when the entity failure is NonFixable but no individual has a NonFixable failure",
        application = tdApp.failedNonFixableAfterOutcome,
        individuals = Seq(tdInd1.failedFixable, tdInd2.approved),
        expectedEmails = Seq(expectedApplicantEmail(tdApp.failedNonFixableAfterOutcome))
      ),
      TestCase(
        description = "sends no emails when the application has already been processed (emailsProcessed = true)",
        application = tdApp.failedNonFixableAfterEmailsProcessed,
        individuals = Seq(tdInd1.failedNonFixableEmailSent, tdInd2.failedNonFixableEmailSent),
        expectedEmails = Seq.empty
      ),
      TestCase(
        description = "sends only the remaining individual email when one individual was already emailed in a prior run",
        application = tdApp.failedNonFixableAfterEmailSent,
        individuals = Seq(tdInd1.failedNonFixableEmailSent, tdInd2.failedNonFixable),
        expectedEmails = Seq(expectedIndividualEmail(tdInd2.failedNonFixable))
      ),
      TestCase(
        description = "sends only the applicant email when SoleTrader and the only individual is the applicant",
        application = soleTraderApp,
        individuals = Seq(soleTraderInd1),
        expectedEmails = Seq(expectedApplicantEmail(soleTraderApp))
      )
    ).foreach: tc =>
      tc.description in:
        tc.expectedEmails.foreach(EmailStubs.stubSendEmail(_))
        applicationRepo.upsert(tc.application).futureValue
        tc.individuals.foreach(individualRepo.upsert(_).futureValue)

        emailService.processEmails().futureValue

        EmailStubs.verifySendEmail(count = tc.expectedEmails.size)
  }
