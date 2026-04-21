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
import uk.gov.hmrc.agentregistration.shared.risking.IndividualRiskingResponse
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistrationrisking.action.Actions
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo

import javax.inject.Inject

class GetIndividualController @Inject() (
  actions: Actions,
  cc: ControllerComponents,
  individualForRiskingRepo: IndividualForRiskingRepo
)
extends BackendController(cc):

  def getIndividualRiskingResponse(personReference: PersonReference): Action[AnyContent] = actions
    .authorised
    .async:
      individualForRiskingRepo
        .findByPersonReference(personReference)
        .map:
          case Some(individual) => Ok(Json.toJson(toIndividualRiskingResponse(individual)))
          case None => NoContent

  private def toIndividualRiskingResponse(individual: IndividualForRisking): IndividualRiskingResponse = IndividualRiskingResponse(
    personReference = individual.individualProvidedDetails.personReference,
    status = individual.status,
    failures = individual.failures
  )
