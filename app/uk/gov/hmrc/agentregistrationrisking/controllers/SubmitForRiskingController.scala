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

package uk.gov.hmrc.agentregistrationrisking.controllers

import play.api.mvc.Action
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.IndividualData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.SubmitForRiskingRequest
import uk.gov.hmrc.agentregistrationrisking.action.Actions
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.OverallStatus
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.services.EmailServiceForSubmissionConfirmations

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton()
class SubmitForRiskingController @Inject() (
  actions: Actions,
  cc: ControllerComponents,
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  individualForRiskingRepo: IndividualForRiskingRepo,
  emailServiceForSubmissionConfirmations: EmailServiceForSubmissionConfirmations
)(using
  ExecutionContext,
  Clock
)
extends BackendController(cc):

  def submitForRisking(): Action[SubmitForRiskingRequest] =
    actions
      .authorised
      .async(parse.json[SubmitForRiskingRequest]):
        implicit request =>
          val now = Instant.now(summon[Clock])
          val application: ApplicationForRisking = makeApplicationForRisking(request.body, now)
          for
            _ <- applicationForRiskingRepo.upsert(application)
            _ <- individualForRiskingRepo.insertMany(makeIndividualForRiskingList(request.body, now))
            _ <- emailServiceForSubmissionConfirmations.sendSubmissionConfirmationEmail(application)
          yield Created

  private def makeApplicationForRisking(
    submitForRiskingRequest: SubmitForRiskingRequest,
    createdAt: Instant
  ): ApplicationForRisking = ApplicationForRisking(
    applicationReference = submitForRiskingRequest.applicationData.applicationReference,
    riskingFileName = None,
    applicationData = submitForRiskingRequest.applicationData,
    createdAt = createdAt,
    lastUpdatedAt = createdAt,
    entityRiskingResult = None,
    isSubscribed = false,
    isEmailSent = false,
    overallStatus = OverallStatus(
      riskingOutcome = None,
      emailsProcessed = false,
      backendNotified = false
    ),
    correctiveActionExpiryDate = None
  )

  private def makeIndividualForRiskingList(
    submitForRiskingRequest: SubmitForRiskingRequest,
    createdAt: Instant
  ) = submitForRiskingRequest.individuals.map(individualProvidedDetails =>
    makeIndividualForRisking(
      applicationReference = submitForRiskingRequest.applicationData.applicationReference,
      individualData = individualProvidedDetails,
      createdAt = createdAt
    )
  )

  private def makeIndividualForRisking(
    applicationReference: ApplicationReference,
    individualData: IndividualData,
    createdAt: Instant
  ): IndividualForRisking = IndividualForRisking(
    personReference = individualData.personReference,
    applicationReference = applicationReference,
    individualData = individualData,
    createdAt = createdAt,
    lastUpdatedAt = createdAt,
    individualRiskingResult = None,
    isEmailSent = false
  )
