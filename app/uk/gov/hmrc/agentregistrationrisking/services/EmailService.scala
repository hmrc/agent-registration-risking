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
import uk.gov.hmrc.agentregistrationrisking.connectors.EmailConnector
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.EmailInformation
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
  applicationForRiskingRepo: ApplicationForRiskingRepo
)(using ExecutionContext)
extends RequestAwareLogging:

  def sendEmail(emailInformation: EmailInformation)(using RequestHeader): Future[Unit] =
    emailConnector.sendEmail(emailInformation)

  def sendRegisteredEmail(application: ApplicationForRisking)(using RequestHeader): Future[Unit] =
    sendEmail(emailInformation(EmailService.registrationSuccessTemplateId, application))

  def findAndSendRegisteredEmail(): Future[Unit] =
    given RequestHeader = EmptyRequest.emptyRequestHeader
    for
      applications <- applicationForRiskingRepo.findSubscribedReadyForSuccessEmail()
      _ = logger.info(s"Found ${applications.size} subscribed applications ready for success email")
      _ <- Future.traverse(applications): application =>
        // TODO: introduce a retry send emails for failed emails
        sendRegisteredEmail(application)
          .recover:
            case ex => logger.warn(s"Failed to send registered email for application ${application._id.value} - marking as sent to avoid unbounded retries", ex)
          .flatMap(_ => applicationForRiskingRepo.updateEmailSent(application._id))
    yield ()

  private def emailInformation(
    templateId: String,
    application: ApplicationForRisking
  ): EmailInformation =
    val agentApplication = application.agentApplication
    val agentDetails = agentApplication.getAgentDetails
    EmailInformation(
      to = Seq(agentDetails.getAgentEmailAddress.getEmailAddress),
      templateId = templateId,
      parameters = Map(
        "agentName" -> agentApplication.getApplicantContactDetails.applicantName.value,
        "applicationRef" -> agentApplication.applicationReference.value,
        "businessName" -> agentDetails.businessName.getAgentBusinessName
      )
    )

object EmailService:

  val registrationSuccessTemplateId: String = "agent_registration_success"
