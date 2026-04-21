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
import uk.gov.hmrc.agentregistrationrisking.action.Actions
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo

import javax.inject.Inject
import javax.inject.Singleton

@Singleton()
class GetApplicationStatusController @Inject() (
  actions: Actions,
  cc: ControllerComponents,
  applicationForRiskingRepo: ApplicationForRiskingRepo
)
extends BackendController(cc):

  def getApplicationStatus(applicationReference: ApplicationReference): Action[AnyContent] = actions
    .authorised
    .async:
      implicit request =>
        applicationForRiskingRepo
          .findByApplicationReference(applicationReference)
          .map:
            case Some(application) =>
              Ok(
                Json.obj(
                  "status" -> application.status.toString
                )
              )
            case None => NotFound
