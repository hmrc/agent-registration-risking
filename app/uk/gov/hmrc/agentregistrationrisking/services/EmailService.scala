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
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationrisking.connectors.EmailConnector
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.model.EmailTemplateId
import uk.gov.hmrc.agentregistrationrisking.model.SendEmailRequest
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.util.EmptyRequest
import uk.gov.hmrc.agentregistrationrisking.util.ProcessInSequence
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class EmailService @Inject() (
  emailConnector: EmailConnector,
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  individualForRiskingRepo: IndividualForRiskingRepo,
  applicationStatusService: ApplicationStatusService
)(using ExecutionContext)
extends RequestAwareLogging:

  def sendEmailsForApprovedApplications(): Future[Unit] =
    given RequestHeader = EmptyRequest.emptyRequestHeader // TODO: make this a param

    for
      applications: Seq[ApplicationForRisking] <- applicationForRiskingRepo.findSubscribedReadyForSuccessEmail()
      applicationCount: Int = applications.size
      _ = logger.info(s"Found ${applicationCount} subscribed applications ready for success email")
      emailsSentSuccessCount <- ProcessInSequence
        .processInSequence(applications): application =>
          processEmailForApproved(application)
            .map(_ => true)
            .recover:
              case ex =>
                logger.error(
                  s"Failed to send registered email for application ${application.applicationReference.value} - will retry on next scheduler tick",
                  ex
                )
                false
        .map(_.count(identity))
      _ = logger.info(s"Sent $emailsSentSuccessCount/$applicationCount emails")
    yield ()

  private def processEmailForApproved(application: ApplicationForRisking)(using RequestHeader): Future[Unit] =
    for
      _ <- sendRegisteredEmail(application)
      _ <- applicationForRiskingRepo.upsert(application.copy(isEmailSent = true))
    yield ()

  def sendEmailsForFailedNonFixable(): Future[Unit] =
    given RequestHeader = EmptyRequest.emptyRequestHeader // TODO

    for
      applicationsWithIndividuals: Seq[ApplicationWithIndividuals] <- applicationForRiskingRepo.findRequiringEmailProcessingForFailedNonFixable()
      _ = logger.info(s"Found ${applicationsWithIndividuals.size} FailedNonFixable applications ready for failure emails")
      _ <- ProcessInSequence.processInSequence(applicationsWithIndividuals)(processEmailsForFailedNonFixable)
    yield ()

  private def sendEmail(emailInformation: SendEmailRequest)(using RequestHeader): Future[Unit] = emailConnector.sendEmail(emailInformation)

  private def sendRegisteredEmail(application: ApplicationForRisking)(using RequestHeader): Future[Unit] = sendEmail(
    applicantSuccessEmailInformation(application)
  )

  private def processEmailsForFailedNonFixable(appWithIndividuals: ApplicationWithIndividuals)(using RequestHeader): Future[Unit] =
    val application = appWithIndividuals.application
    val individuals: Seq[IndividualForRisking] =
      application.agentApplication.businessType match
        case BusinessType.SoleTrader => appWithIndividuals.individuals.filterNot(isIndividualTheApplicant(_, application))
        case _ => appWithIndividuals.individuals
    for
      _ <- sendFailedNonFixableEmailForApplicant(application)
      _ <- ProcessInSequence.processInSequence(individuals)(sendIndividualFailureEmailIfPending)
//      _ <- updateOverallStatusEmailSent(application)
    yield ()

  private def sendFailedNonFixableEmailForApplicant(application: ApplicationForRisking)(using RequestHeader): Future[Unit] =
    if application.isEmailSent then Future.unit
    else
      sendEmail(applicantNonFixableFailureEmailInformation(application))
        .flatMap(_ => applicationForRiskingRepo.upsert(application.copy(isEmailSent = true)))
        .recover:
          case ex =>
            logger.warn(
              s"Failed to send applicant failure email for application ${application.applicationReference.value} - will retry on next scheduler tick",
              ex
            )

  private def sendIndividualFailureEmailIfPending(individual: IndividualForRisking)(using RequestHeader): Future[Unit] =
    if individual.isEmailSent then Future.unit
    else
      sendEmail(individualEmailInformation(EmailTemplateId.IndividualNonFixableFailure, individual))
        .flatMap(_ => individualForRiskingRepo.updateEmailSent(individual.personReference).map(_ => ()))
        .recover:
          case ex =>
            logger.warn(
              s"Failed to send individual failure email for individual ${individual.personReference.value} (application ${individual.applicationReference.value}) - will retry on next scheduler tick",
              ex
            )

  private def isIndividualTheApplicant(
    individual: IndividualForRisking,
    application: ApplicationForRisking
  ): Boolean =
    val applicantEmail = application.agentApplication.applicantContactDetails
      .flatMap(_.applicantEmailAddress)
      .map(_.emailAddress.value)
    val individualEmail = individual.individualProvidedDetails.emailAddress
      .map(_.emailAddress.value)
    applicantEmail.isDefined && applicantEmail === individualEmail

  private def applicantSuccessEmailInformation(application: ApplicationForRisking): SendEmailRequest =
    val agentApplication = application.agentApplication
    val agentDetails = agentApplication.getAgentDetails
    SendEmailRequest(
      to = Seq(EmailAddress(agentDetails.getAgentEmailAddress.getEmailAddress)),
      templateId = EmailTemplateId.RegistrationSuccess,
      parameters = Map(
        "agentName" -> agentApplication.getApplicantContactDetails.applicantName.value,
        "applicationRef" -> agentApplication.applicationReference.value,
        "businessName" -> agentDetails.businessName.getAgentBusinessName
      )
    )

  private def applicantNonFixableFailureEmailInformation(application: ApplicationForRisking): SendEmailRequest =
    val agentApplication = application.agentApplication
    SendEmailRequest(
      to = Seq(EmailAddress(agentApplication.getAgentDetails.getAgentEmailAddress.getEmailAddress)),
      templateId = EmailTemplateId.ApplicationNonFixableFailure,
      parameters = Map(
        "agentName" -> agentApplication.getApplicantContactDetails.applicantName.value,
        "applicationRef" -> agentApplication.applicationReference.value
      )
    )

  private def individualEmailInformation(
    templateId: EmailTemplateId,
    individual: IndividualForRisking
  ): SendEmailRequest =
    val individualDetails = individual.individualProvidedDetails
    SendEmailRequest(
      to = Seq(individualDetails.getEmailAddress.emailAddress),
      templateId = templateId,
      parameters = Map(
        "individualName" -> individualDetails.individualName.value
      )
    )
