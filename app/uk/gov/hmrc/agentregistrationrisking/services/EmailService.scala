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
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistrationrisking.connectors.EmailConnector
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.model.EmailInformation
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.util.EmptyRequest
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class EmailService @Inject() (
  emailConnector: EmailConnector,
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  applicationStatusService: ApplicationStatusService
)(using ExecutionContext)
extends RequestAwareLogging:

  def sendEmail(emailInformation: EmailInformation)(using RequestHeader): Future[Unit] = emailConnector.sendEmail(emailInformation)

  def sendRegisteredEmail(application: ApplicationForRisking)(using RequestHeader): Future[Unit] = sendEmail(applicantSuccessEmailInformation(application))

  def sendApplicantNonFixableFailureEmail(application: ApplicationForRisking)(using RequestHeader): Future[Unit] = sendEmail(
    applicantNonFixableFailureEmailInformation(application)
  )

  def sendIndividualNonFixableFailureEmail(
    individual: IndividualForRisking
  )(using RequestHeader): Future[Unit] = sendEmail(individualEmailInformation(
    EmailService.individualNonFixableFailureTemplateId,
    individual
  ))

  def findAndSendRegisteredEmail(): Future[Unit] =
    given RequestHeader = EmptyRequest.emptyRequestHeader
    for
      applications <- applicationForRiskingRepo.findSubscribedReadyForSuccessEmail()
      _ = logger.info(s"Found ${applications.size} subscribed applications ready for success email")
      _ <-
        Future.traverse(applications): application =>
          // TODO: introduce a retry send emails for failed emails
          sendRegisteredEmail(application)
            .recover:
              case ex =>
                logger.warn(s"Failed to send registered email for application ${application._id.value} - marking as sent to avoid unbounded retries", ex)
            .flatMap(_ => applicationForRiskingRepo.updateEmailSent(application._id))
    yield ()

  def findAndSendNonFixableFailureEmails(): Future[Unit] =
    given RequestHeader = EmptyRequest.emptyRequestHeader
    for
      candidates <- applicationStatusService.getApplicationsReadyForFailureEmailCheckWithIndividuals
      nonFixable = applicationStatusService.getNonFixableApplicationsWithIndividuals(candidates)
      _ = logger.info(s"Found ${nonFixable.size} FailedNonFixable applications ready for failure emails")
      _ <- Future.traverse(nonFixable)(sendNonFixableFailureEmailsForApplication)
    yield ()

  private def sendNonFixableFailureEmailsForApplication(appWithIndividuals: ApplicationWithIndividuals)(using RequestHeader): Future[Unit] =
    val application = appWithIndividuals.application
    // Sole trader edge case: the (single) individual is the applicant, who already received the applicant email,
    // so skip the individual email to avoid duplicates.
    val individuals: Seq[IndividualForRisking] =
      application.agentApplication.businessType match
        case BusinessType.SoleTrader => Nil
        case _ => appWithIndividuals.individuals
    // TODO: introduce a retry cap. For now we always mark isEmailSent=true after one round of attempts to avoid
    // unbounded retries. Failed sends are visible in WARN logs.
    for
      _ <- sendApplicantNonFixableFailureEmail(application)
        .recover:
          case ex => logger.warn(s"Failed to send applicant failure email for application ${application._id.value}", ex)
      _ <-
        Future.traverse(individuals): individual =>
          sendIndividualNonFixableFailureEmail(individual)
            .recover:
              case ex =>
                logger.warn(s"Failed to send individual failure email for individual ${individual._id.value} (application ${application._id.value})", ex)
      _ <- applicationForRiskingRepo.updateEmailSent(application._id)
    yield ()

  private def applicantSuccessEmailInformation(application: ApplicationForRisking): EmailInformation =
    val agentApplication = application.agentApplication
    val agentDetails = agentApplication.getAgentDetails
    EmailInformation(
      to = Seq(agentDetails.getAgentEmailAddress.getEmailAddress),
      templateId = EmailService.registrationSuccessTemplateId,
      parameters = Map(
        "agentName" -> agentApplication.getApplicantContactDetails.applicantName.value,
        "applicationRef" -> agentApplication.applicationReference.value,
        "businessName" -> agentDetails.businessName.getAgentBusinessName
      )
    )

  private def applicantNonFixableFailureEmailInformation(application: ApplicationForRisking): EmailInformation =
    val agentApplication = application.agentApplication
    EmailInformation(
      to = Seq(agentApplication.getAgentDetails.getAgentEmailAddress.getEmailAddress),
      templateId = EmailService.applicationNonFixableFailureTemplateId,
      parameters = Map(
        "agentName" -> agentApplication.getApplicantContactDetails.applicantName.value,
        "applicationRef" -> agentApplication.applicationReference.value
      )
    )

  private def individualEmailInformation(
    templateId: String,
    individual: IndividualForRisking
  ): EmailInformation =
    val individualDetails = individual.individualProvidedDetails
    EmailInformation(
      to = Seq(individualDetails.getEmailAddress.emailAddress.value),
      templateId = templateId,
      parameters = Map(
        "individualName" -> individualDetails.individualName.value
      )
    )

object EmailService:

  val registrationSuccessTemplateId: String = "agent_registration_success"
  val applicationNonFixableFailureTemplateId: String = "agent_registration_application_non_fixable_failure"
  val individualNonFixableFailureTemplateId: String = "agent_registration_individual_non_fixable_failure"
