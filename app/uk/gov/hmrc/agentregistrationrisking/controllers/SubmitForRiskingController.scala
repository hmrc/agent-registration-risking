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
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.risking.SubmitForRiskingRequest
import uk.gov.hmrc.agentregistrationrisking.action.Actions
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo

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
  individualForRiskingRepo: IndividualForRiskingRepo
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
          for
            _ <- applicationForRiskingRepo.upsert(makeApplicationForRisking(request.body, now))
            _ <- individualForRiskingRepo.insertMany(makeIndividualForRiskingList(request.body, now))
          yield Created

  private def makeApplicationForRisking(
    submitForRiskingRequest: SubmitForRiskingRequest,
    createdAt: Instant
  ): ApplicationForRisking = ApplicationForRisking(
    applicationReference = submitForRiskingRequest.agentApplication.applicationReference,
    riskingFileName = None,
    agentApplication = submitForRiskingRequest.agentApplication,
    createdAt = createdAt,
    lastUpdatedAt = createdAt,
    entityRiskingResult = None,
    isSubscribed = false,
    isEmailSent = false
  )

  private def makeIndividualForRiskingList(
    submitForRiskingRequest: SubmitForRiskingRequest,
    createdAt: Instant
  ) = submitForRiskingRequest.individuals.map(individualProvidedDetails =>
    makeIndividualForRisking(
      applicationReference = submitForRiskingRequest.agentApplication.applicationReference,
      individualProvidedDetails = individualProvidedDetails,
      createdAt = createdAt
    )
  )

  private def makeIndividualForRisking(
    applicationReference: ApplicationReference,
    individualProvidedDetails: IndividualProvidedDetails,
    createdAt: Instant
  ): IndividualForRisking = IndividualForRisking(
    personReference = individualProvidedDetails.personReference,
    applicationReference = applicationReference,
    individualProvidedDetails = individualProvidedDetails,
    createdAt = createdAt,
    lastUpdatedAt = createdAt,
    individualRiskingResult = None
  )
