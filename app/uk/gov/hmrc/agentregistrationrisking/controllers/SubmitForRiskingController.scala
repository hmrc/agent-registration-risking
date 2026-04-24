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
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.risking.SubmitForRiskingRequest
import uk.gov.hmrc.agentregistrationrisking.action.Actions
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRiskingId
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRiskingIdGenerator
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRiskingId
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRiskingIdGenerator
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
  individualForRiskingRepo: IndividualForRiskingRepo,
  applicationForRiskingIdGenerator: ApplicationForRiskingIdGenerator,
  individualForRiskingIdGenerator: IndividualForRiskingIdGenerator
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
          val appId = applicationForRiskingIdGenerator.nextApplicationId()
          val application = toApplicationForRisking(
            request.body,
            appId,
            now
          )
          val individuals = request.body.individuals.map(toIndividualForRisking(_, appId, now))
          for
            _ <- applicationForRiskingRepo.upsert(application)
            _ <- Future.traverse(individuals)(individualForRiskingRepo.upsert)
          yield Created

  private def toApplicationForRisking(
    request: SubmitForRiskingRequest,
    appId: ApplicationForRiskingId,
    now: Instant
  ): ApplicationForRisking = ApplicationForRisking(
    _id = appId,
    agentApplication = request.agentApplication,
    createdAt = now,
    lastUpdatedAt = now,
    riskingFileId = None,
    failures = None,
    isSubscribed = false,
    isEmailSent = false
  )

  private def toIndividualForRisking(
    individual: IndividualProvidedDetails,
    appId: ApplicationForRiskingId,
    now: Instant
  ): IndividualForRisking = IndividualForRisking(
    _id = individualForRiskingIdGenerator.nextIndividualId(),
    applicationForRiskingId = appId,
    individualProvidedDetails = individual,
    createdAt = now,
    lastUpdatedAt = now,
    failures = None
  )
