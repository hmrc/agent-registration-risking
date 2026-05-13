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
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationrisking.connectors.EmailConnector
import uk.gov.hmrc.agentregistrationrisking.model.*
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.util.ProcessInSequence
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class EmailServiceForFailedNonFixable @Inject() (
  emailConnector: EmailConnector,
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  individualForRiskingRepo: IndividualForRiskingRepo
)(using ExecutionContext)
extends RequestAwareLogging:

  private val emailTemplateId: EmailTemplateId = EmailTemplateId.ApplicationNonFixableFailure

  def processEmails()(using requestHeader: RequestHeader): Future[Unit] =
    for
      applicationsWithIndividuals: Seq[ApplicationWithIndividuals] <- applicationForRiskingRepo.findRequiringEmailProcessingForFailedNonFixable()
      applicationsCount: Int = applicationsWithIndividuals.size
      _ = logger.info(s"Found $applicationsCount FailedNonFixable applications with individuals ready to process emails")
      successfullyProcessedCount <-
        ProcessInSequence.processAllInSequence(applicationsWithIndividuals)(process):
          case (ex, applicationWithIndividuals) =>
            logger.error(
              s"Failed to process emails for FailedNonFixable application: ${applicationWithIndividuals.application.applicationReference}",
              ex
            )
      _ = logger.info(s"Processed emails for $successfullyProcessedCount/$applicationsCount FailedNonFixable applications")
    yield ()

  private def process(applicationWithIndividuals: ApplicationWithIndividuals)(using RequestHeader): Future[Unit] =
    val application: ApplicationForRisking = applicationWithIndividuals.application
    val individuals: Seq[IndividualForRisking] =
      application.applicationData.businessType match
        case BusinessType.SoleTrader => applicationWithIndividuals.individuals.filterNot(isIndividualTheApplicant(_, application))
        case _ => applicationWithIndividuals.individuals

    for
      updatedApplication <- process(application)
      _ <-
        ProcessInSequence
          .processInSequence(individuals.filter(_.isEmailSent === false))(process)
      _ <- applicationForRiskingRepo
        .upsert(updatedApplication.modify(_.overallStatus.emailsProcessed).setTo(true))
    yield ()

  private def process(application: ApplicationForRisking)(using RequestHeader): Future[ApplicationForRisking] =
    if application.isEmailSent
    then Future.successful(application)
    else
      for
        sendEmailRequest <- Future.successful(makeSendEmailRequest(application))
        _ = logger.info(s"Sending ${sendEmailRequest.templateId} email for ${application.applicationReference}")
        _ <- emailConnector.sendEmail(sendEmailRequest)
        updatedApplication = application.copy(isEmailSent = true)
        _ <- applicationForRiskingRepo.upsert(updatedApplication)
        _ = logger.info(s"Sent ${sendEmailRequest.templateId} email for ${application.applicationReference}")
      yield updatedApplication

  private def process(individual: IndividualForRisking)(using RequestHeader): Future[Unit] =
    for
      sendEmailRequest <- Future.successful(makeSendEmailRequest(individual))
      _ = logger.info(s"Sending ${sendEmailRequest.templateId} email for ${individual.applicationReference} ${individual.personReference}")
      _ <- emailConnector.sendEmail(sendEmailRequest)
      _ <- individualForRiskingRepo.upsert(individual.copy(isEmailSent = true))
      _ = logger.info(s"Sent ${sendEmailRequest.templateId} email for ${individual.applicationReference} ${individual.personReference}")
    yield ()

  private def isIndividualTheApplicant(
    individual: IndividualForRisking,
    application: ApplicationForRisking
  ): Boolean =
    application
      .applicationData
      .applicantContactDetails
      .applicantEmailAddress === individual
      .individualProvidedDetails
      .getEmailAddress
      .emailAddress

  private def makeSendEmailRequest(application: ApplicationForRisking): SendEmailRequest =
    val agentApplication = application.applicationData
    SendEmailRequest(
      to = Seq(agentApplication.agentDetails.agentEmailAddress.getEmailAddress),
      templateId = emailTemplateId,
      parameters = Map(
        "agentName" -> agentApplication.applicantContactDetails.applicantName.value,
        "applicationRef" -> agentApplication.applicationReference.value
      )
    )

  private def makeSendEmailRequest(
    individual: IndividualForRisking
  ): SendEmailRequest =
    val individualDetails = individual.individualProvidedDetails
    SendEmailRequest(
      to = Seq(individualDetails.getEmailAddress.emailAddress),
      templateId = EmailTemplateId.IndividualNonFixableFailure,
      parameters = Map(
        "individualName" -> individualDetails.individualName.value
      )
    )
