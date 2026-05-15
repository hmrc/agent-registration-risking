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
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.AgentDetailsData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicationData
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.connectors.EmailConnector
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.EmailTemplateId
import uk.gov.hmrc.agentregistrationrisking.model.SendEmailRequest
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class EmailServiceForSubmissionConfirmations @Inject() (
  emailConnector: EmailConnector,
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  appConfig: AppConfig
)(using ExecutionContext)
extends RequestAwareLogging:

  private val emailTemplateId: EmailTemplateId = EmailTemplateId.SubmissionConfirmation

  def sendSubmissionConfirmationEmail(application: ApplicationForRisking)(using RequestHeader): Future[Unit] =
    val sendEmailRequest = makeSendEmailRequest(application)
    logger.info(s"Sending ${sendEmailRequest.templateId} email for ${application.applicationReference}")
    emailConnector.sendEmail(sendEmailRequest)

  private def makeSendEmailRequest(application: ApplicationForRisking): SendEmailRequest =
    val agentApplication: ApplicationData = application.applicationData
    val agentDetails: AgentDetailsData = agentApplication.agentDetails
    SendEmailRequest(
      to = Seq(agentDetails.agentEmailAddress),
      templateId = emailTemplateId,
      parameters = Map(
        "agentName" -> agentApplication.applicantContactDetails.applicantName.value,
        "applicationRef" -> agentApplication.applicationReference.value,
        "applicationProcessingTime" -> appConfig.Email.applicationProcessingTime
      )
    )
