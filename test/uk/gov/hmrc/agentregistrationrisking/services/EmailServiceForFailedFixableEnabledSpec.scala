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
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdIndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.EmailStubs

class EmailServiceForFailedFixableEnabledSpec
extends ISpec:

  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "features.fixable-failures" -> true
  )

  private val emailServiceForFailedFixable: EmailServiceForFailedFixable = app.injector.instanceOf[EmailServiceForFailedFixable]
  private val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  private val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  private given RequestHeader = tdAll.fakeBackendRequest

  override def beforeEach(): Unit =
    super.beforeEach()
    applicationForRiskingRepo.collection.drop().toFuture.futureValue
    individualForRiskingRepo.collection.drop().toFuture.futureValue
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

  private val tdRisking: TdRisking = TdRisking.make("EmailServiceForFailedFixableEnabledSpec")
  private val tdApplicationForRisking: TdApplicationForRisking = tdRisking.tdApplicationForRisking
  private val tdIndividualForRisking1: TdIndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1
  private val tdIndividualForRisking2: TdIndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2
  private val tdIndividualForRisking3: TdIndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking3

  private val soleTraderApp: ApplicationForRisking = tdApplicationForRisking.receivedRiskingResults.failedFixableAfterOutcome
    .modify(_.applicationData.businessType)
    .setTo(BusinessType.SoleTrader)
  private val soleTraderInd1: IndividualForRisking = tdIndividualForRisking1.receivedRiskingResults.failedFixable
    .modify(_.individualData.emailAddress)
    .setTo(soleTraderApp.applicationData.applicantContactDetails.applicantEmailAddress)

  "processEmails" - {

    List(
      TestCase(
        description = "sends 1 applicant email and 1 individual email when only 1 of 3 individuals has a Fixable failure",
        application = tdApplicationForRisking.receivedRiskingResults.failedFixableAfterOutcome,
        individuals = Seq(
          tdIndividualForRisking1.receivedRiskingResults.failedFixable,
          tdIndividualForRisking2.receivedRiskingResults.approved,
          tdIndividualForRisking3.receivedRiskingResults.approved
        ),
        expectedEmails = Seq(
          expectedApplicantEmail(tdApplicationForRisking.receivedRiskingResults.failedFixableAfterOutcome),
          expectedIndividualEmail(tdIndividualForRisking1.receivedRiskingResults.failedFixable)
        )
      ),
      TestCase(
        description = "sends 1 applicant email and 2 individual emails when 2 of 3 individuals have a Fixable failure",
        application = tdApplicationForRisking.receivedRiskingResults.failedFixableAfterOutcome,
        individuals = Seq(
          tdIndividualForRisking1.receivedRiskingResults.failedFixable,
          tdIndividualForRisking2.receivedRiskingResults.failedFixable,
          tdIndividualForRisking3.receivedRiskingResults.approved
        ),
        expectedEmails = Seq(
          expectedApplicantEmail(tdApplicationForRisking.receivedRiskingResults.failedFixableAfterOutcome),
          expectedIndividualEmail(tdIndividualForRisking1.receivedRiskingResults.failedFixable),
          expectedIndividualEmail(tdIndividualForRisking2.receivedRiskingResults.failedFixable)
        )
      ),
      TestCase(
        description = "sends only the applicant email when the entity failure is Fixable but no individual has a Fixable failure",
        application = tdApplicationForRisking.receivedRiskingResults.failedFixableAfterOutcome,
        individuals = Seq(tdIndividualForRisking1.receivedRiskingResults.approved, tdIndividualForRisking2.receivedRiskingResults.approved),
        expectedEmails = Seq(expectedApplicantEmail(tdApplicationForRisking.receivedRiskingResults.failedFixableAfterOutcome))
      ),
      TestCase(
        description = "sends no emails when the application has already been processed (emailsProcessed = true)",
        application = tdApplicationForRisking.receivedRiskingResults.failedFixableAfterEmailSent,
        individuals = Seq(
          tdIndividualForRisking1.receivedRiskingResults.failedFixableEmailSent,
          tdIndividualForRisking2.receivedRiskingResults.failedFixableEmailSent
        ),
        expectedEmails = Seq.empty
      ),
      TestCase(
        description =
          "sends only the remaining individual email when the entity email is already sent but one individual was not yet emailed (crash-recovery intermediate state)",
        application = tdApplicationForRisking.receivedRiskingResults.failedFixableAfterOutcome.copy(isEmailSent = true),
        individuals = Seq(
          tdIndividualForRisking1.receivedRiskingResults.failedFixableEmailSent,
          tdIndividualForRisking2.receivedRiskingResults.failedFixable
        ),
        expectedEmails = Seq(expectedIndividualEmail(tdIndividualForRisking2.receivedRiskingResults.failedFixable))
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
        applicationForRiskingRepo.upsert(tc.application).futureValue
        tc.individuals.foreach(individualForRiskingRepo.upsert(_).futureValue)

        emailServiceForFailedFixable.processEmails().futureValue

        EmailStubs.verifySendEmail(count = tc.expectedEmails.size)

    "leaves overallStatus.emailsProcessed=false and emailSentAt=None when one individual email fails mid-batch — atomic set must not run so the next scheduler run retries" in:
      val application: ApplicationForRisking = tdApplicationForRisking.receivedRiskingResults.failedFixableAfterOutcome
      val individual1: IndividualForRisking = tdIndividualForRisking1.receivedRiskingResults.failedFixable
      val individual2: IndividualForRisking = tdIndividualForRisking2.receivedRiskingResults.failedFixable

      EmailStubs.stubSendEmail(expectedApplicantEmail(application))
      EmailStubs.stubSendEmail(expectedIndividualEmail(individual1))
      EmailStubs.stubSendEmailFailure(expectedIndividualEmail(individual2))

      applicationForRiskingRepo.upsert(application).futureValue
      individualForRiskingRepo.upsert(individual1).futureValue
      individualForRiskingRepo.upsert(individual2).futureValue

      emailServiceForFailedFixable.processEmails().futureValue

      val persistedApp: ApplicationForRisking = applicationForRiskingRepo.findById(application.applicationReference).futureValue.value
      persistedApp.isEmailSent shouldBe true withClue "entity email was sent so isEmailSent should have been flipped by the first upsert"
      persistedApp.overallStatus.emailsProcessed shouldBe false withClue "atomic final upsert must NOT have run — individual2 email failed"
      persistedApp.overallStatus.emailsSentAt shouldBe None withClue "atomic final upsert must NOT have run"

      individualForRiskingRepo.findByApplicationReference(
        application.applicationReference
      ).futureValue.find(_.personReference == individual1.personReference).value.isEmailSent shouldBe true
      individualForRiskingRepo.findByApplicationReference(application.applicationReference).futureValue.find(
        _.personReference == individual2.personReference
      ).value.isEmailSent shouldBe false withClue "individual2 email failed — its isEmailSent flag must NOT flip"

    "leaves the whole record untouched when the entity email fails — no individual emails attempted, atomic set never reached" in:
      val application: ApplicationForRisking = tdApplicationForRisking.receivedRiskingResults.failedFixableAfterOutcome
      val individual1: IndividualForRisking = tdIndividualForRisking1.receivedRiskingResults.failedFixable
      val individual2: IndividualForRisking = tdIndividualForRisking2.receivedRiskingResults.failedFixable

      EmailStubs.stubSendEmailFailure(expectedApplicantEmail(application))

      applicationForRiskingRepo.upsert(application).futureValue
      individualForRiskingRepo.upsert(individual1).futureValue
      individualForRiskingRepo.upsert(individual2).futureValue

      emailServiceForFailedFixable.processEmails().futureValue

      val persistedApp: ApplicationForRisking = applicationForRiskingRepo.findById(application.applicationReference).futureValue.value
      persistedApp.isEmailSent shouldBe false withClue "entity email failed — no upsert should have run"
      persistedApp.overallStatus.emailsProcessed shouldBe false
      persistedApp.overallStatus.emailsSentAt shouldBe None

      individualForRiskingRepo.findByApplicationReference(application.applicationReference).futureValue.foreach: individual =>
        individual.isEmailSent shouldBe false withClue s"individual ${individual.personReference} — entity email failed, individual emails must not have been attempted"

    "completes the atomic final upsert on the next scheduler run after a mid-batch crash — sends the remaining individual email then flips emailsProcessed+emailSentAt together" in:
      val applicationCrashedMidBatch: ApplicationForRisking = tdApplicationForRisking
        .receivedRiskingResults
        .failedFixableAfterOutcome
        .copy(isEmailSent = true)
      val individual1EmailedInPriorRun: IndividualForRisking = tdIndividualForRisking1.receivedRiskingResults.failedFixableEmailSent
      val individual2StillPending: IndividualForRisking = tdIndividualForRisking2.receivedRiskingResults.failedFixable

      EmailStubs.stubSendEmail(expectedIndividualEmail(individual2StillPending))

      applicationForRiskingRepo.upsert(applicationCrashedMidBatch).futureValue
      individualForRiskingRepo.upsert(individual1EmailedInPriorRun).futureValue
      individualForRiskingRepo.upsert(individual2StillPending).futureValue

      emailServiceForFailedFixable.processEmails().futureValue

      val persistedApp: ApplicationForRisking = applicationForRiskingRepo.findById(applicationCrashedMidBatch.applicationReference).futureValue.value
      persistedApp.overallStatus.emailsProcessed shouldBe true withClue "all emails complete — atomic final upsert must have run"
      persistedApp.overallStatus.emailsSentAt shouldBe defined withClue "atomic final upsert must have set emailSentAt together with emailsProcessed"
      individualForRiskingRepo
        .findByApplicationReference(applicationCrashedMidBatch.applicationReference)
        .futureValue
        .find(_.personReference == individual2StillPending.personReference)
        .value
        .isEmailSent shouldBe true
  }
