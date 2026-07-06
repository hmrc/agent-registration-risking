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
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicationData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.AgentDetailsData
import uk.gov.hmrc.agentregistrationrisking.connectors.EmailConnector
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.EmailTemplateId
import uk.gov.hmrc.agentregistrationrisking.model.SendEmailRequest
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.util.ProcessInSequence
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class EmailServiceForApprovedApplications @Inject() (
  emailConnector: EmailConnector,
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  individualForRiskingRepo: IndividualForRiskingRepo,
  clock: Clock
)(using ExecutionContext)
extends RequestAwareLogging:

  private val emailTemplateId: EmailTemplateId = EmailTemplateId.RegistrationSuccess

  def processEmails()(using RequestHeader): Future[Unit] =
    for
      applications: Seq[ApplicationForRisking] <- applicationForRiskingRepo.findSubscribedReadyForSuccessEmail()
      applicationCount: Int = applications.size
      _ = logger.info(s"Found $applicationCount subscribed applications ready to be sent an email: $emailTemplateId")
      emailsSentSuccessCount <-
        ProcessInSequence
          .processAllInSequence(applications)(process):
            case (ex, application) => logger.error(s"Failed to send email for ${application.applicationReference}: $emailTemplateId", ex)
      _ = logger.info(s"Sent $emailsSentSuccessCount/$applicationCount $emailTemplateId emails")
    yield ()

  private def process(application: ApplicationForRisking)(using RequestHeader): Future[Unit] =
    for
      sendEmailRequest <- Future.successful(makeSendEmailRequest(application))
      _ = logger.info(s"Sending ${sendEmailRequest.templateId} email for ${application.applicationReference}")
      _ <- emailConnector.sendEmail(sendEmailRequest)
      _ <- applicationForRiskingRepo.upsert(
        application
          .copy(isEmailSent = true)
          .modify(_.overallStatus.emailsProcessed)
          .setTo(true)
          .modify(_.overallStatus.emailSentAt)
          .setTo(Some(Instant.now(clock)))
      )
      _ = logger.info(s"Sent ${sendEmailRequest.templateId} email for ${application.applicationReference}")
    yield ()

  private def makeSendEmailRequest(application: ApplicationForRisking): SendEmailRequest =
    val agentApplication: ApplicationData = application.applicationData
    val agentDetails: AgentDetailsData = agentApplication.agentDetails
    SendEmailRequest(
      to = Seq(agentDetails.agentEmailAddress),
      templateId = emailTemplateId,
      parameters = Map(
        "agentName" -> agentApplication.applicantContactDetails.applicantName.value,
        "applicationRef" -> agentApplication.applicationReference.value,
        "businessName" -> agentDetails.businessName.getAgentBusinessName
      )
    )
