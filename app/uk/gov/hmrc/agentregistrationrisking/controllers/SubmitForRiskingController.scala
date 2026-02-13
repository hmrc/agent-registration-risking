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
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.util.Errors
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationrisking.action.Actions
import uk.gov.hmrc.agentregistrationrisking.model.SubmitForRiskingRequest
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton()
class SubmitForRiskingController @Inject() (
  actions: Actions,
  cc: ControllerComponents
)
extends BackendController(cc):

  def submitForRisking(applicationId: AgentApplicationId) =
    actions
      .authorised
      .async(parse.json[SubmitForRiskingRequest]):
        implicit request =>
//          Errors.require(request.internalUserId === request.body.internalUserId.internalUserId, "Only applicant can submit application for risking")

          // TODO: this is just a scaffold, the actual request structure and what this endpoint does needs to be defined

          Future.successful(Accepted)
