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

import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationRiskingResponse
import uk.gov.hmrc.agentregistration.shared.risking.IndividualRiskingResponse
import uk.gov.hmrc.agentregistrationrisking.action.Actions
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class GetApplicationController @Inject() (
  actions: Actions,
  cc: ControllerComponents,
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  individualForRiskingRepo: IndividualForRiskingRepo
)(using ExecutionContext)
extends BackendController(cc):

  def getApplicationRiskingResponse(applicationReference: ApplicationReference): Action[AnyContent] = actions
    .authorised
    .async:
      applicationForRiskingRepo
        .findByApplicationReference(applicationReference)
        .flatMap:
          case None => Future.successful(NoContent)
          case Some(application) =>
            individualForRiskingRepo
              .findByApplicationForRiskingId(application._id)
              .map: individuals =>
                Ok(Json.toJson(toApplicationRiskingResponse(application, individuals)))

  private def toApplicationRiskingResponse(
    application: ApplicationForRisking,
    individuals: Seq[IndividualForRisking]
  ): ApplicationRiskingResponse = ApplicationRiskingResponse(
    applicationReference = application.agentApplication.applicationReference,
    status = application.status,
    isSubscribed = application.isSubscribed,
    individuals = individuals.toList.map(toIndividualRiskingResponse),
    failures = application.failures
  )

  private def toIndividualRiskingResponse(individual: IndividualForRisking): IndividualRiskingResponse = IndividualRiskingResponse(
    personReference = individual.individualProvidedDetails.personReference,
    status = individual.status,
    failures = individual.failures
  )
