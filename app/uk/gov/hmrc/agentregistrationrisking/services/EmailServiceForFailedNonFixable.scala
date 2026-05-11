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
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentDetails
import uk.gov.hmrc.agentregistration.shared.AgentApplication
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
import com.softwaremill.quicklens.modify

@Singleton
class EmailServiceForFailedNonFixable @Inject() (
  emailConnector: EmailConnector,
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  individualForRiskingRepo: IndividualForRiskingRepo,
  applicationStatusService: ApplicationStatusService
)(using ExecutionContext)
extends RequestAwareLogging:

  def processEmails()(using requestHeader: RequestHeader): Future[Unit] =
    for
      applicationsWithIndividuals: Seq[ApplicationWithIndividuals] <- applicationForRiskingRepo.findRequiringEmailProcessingForFailedNonFixable()
      _ = logger.info(s"Found ${applicationsWithIndividuals.size} FailedNonFixable applications ready for failure emails")
      _ <- ProcessInSequence.processInSequence(applicationsWithIndividuals)(process)
    yield ()

  private def process(applicationWithIndividuals: ApplicationWithIndividuals)(using RequestHeader): Future[Unit] =
    val application: ApplicationForRisking = applicationWithIndividuals.application
    val individuals: Seq[IndividualForRisking] =
      application.agentApplication.businessType match
        case BusinessType.SoleTrader => applicationWithIndividuals.individuals.filterNot(isIndividualTheApplicant(_, application))
        case _ => applicationWithIndividuals.individuals

    val processF: Future[Unit] =
      for
        updatedApplication <- process(application)
        _ <-
          ProcessInSequence
            .processInSequence(individuals.filter(_.isEmailSent === false))(process)
        _ <- applicationForRiskingRepo
          .upsert(updatedApplication.modify(_.overallStatus.emailsProcessed).setTo(true))
      yield ()

    processF
  // TODO: logging

  private def process(application: ApplicationForRisking)(using RequestHeader): Future[ApplicationForRisking] =
    if application.isEmailSent
    then Future.successful(application)
    else
      for
        sendEmailRequest <- Future.successful(makeSendEmailRequest(application))
        _ <- emailConnector.sendEmail(sendEmailRequest)
        updatedApplication = application.copy(isEmailSent = true)
        _ <- applicationForRiskingRepo.upsert(updatedApplication)
      yield updatedApplication

  private def process(individual: IndividualForRisking)(using RequestHeader): Future[Unit] =
    for
      sendEmailRequest <- Future.successful(makeSendEmailRequest(individual))
      _ <- emailConnector.sendEmail(sendEmailRequest)
      _ <- individualForRiskingRepo.upsert(individual.copy(isEmailSent = true))
    yield ()

  private def isIndividualTheApplicant(
    individual: IndividualForRisking,
    application: ApplicationForRisking
  ): Boolean =
    val maybeTheSame: Option[Boolean] =
      for
        applicantContactDetails <- application.agentApplication.applicantContactDetails
        applicantEmailAddress <- applicantContactDetails.applicantEmailAddress.map(_.emailAddress)
        individualEmail <- individual.individualProvidedDetails.emailAddress.map(_.emailAddress)
      yield (applicantEmailAddress.value === individualEmail.value)

    maybeTheSame.getOrElse(false)

  private def makeSendEmailRequest(application: ApplicationForRisking): SendEmailRequest =
    val agentApplication = application.agentApplication
    SendEmailRequest(
      to = Seq(EmailAddress(agentApplication.getAgentDetails.getAgentEmailAddress.getEmailAddress)),
      templateId = EmailTemplateId.ApplicationNonFixableFailure,
      parameters = Map(
        "agentName" -> agentApplication.getApplicantContactDetails.applicantName.value,
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
