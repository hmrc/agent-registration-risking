/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationrisking.controllers.smu

import play.api.libs.json.Json
import play.api.libs.typedmap.TypedMap
import play.api.mvc.request.RemoteConnection
import play.api.mvc.request.RequestTarget
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.mvc.Headers
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistrationrisking.action.Actions
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.controllers.BackendController
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.smu.SmuIndividualResponse
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.services.ObjectStoreService

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton()
class SmuViewerController @Inject() (
  cc: ControllerComponents,
  actions: Actions,
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  individualForRiskingRepo: IndividualForRiskingRepo,
  objectStoreService: ObjectStoreService,
  appConfig: AppConfig
)
extends BackendController(cc):

  def findIndividualByPersonReference(personReference: PersonReference): Action[AnyContent] = actions.basicAuthorised.async: request =>
    for
      maybeIndividual: Option[IndividualForRisking] <- individualForRiskingRepo.findById(personReference)
      maybeApp: Option[ApplicationForRisking] <-
        maybeIndividual match
          case Some(indi) => applicationForRiskingRepo.findById(indi.applicationReference)
          case None => Future.successful(None)
    yield (maybeIndividual, maybeApp) match
      case (Some(indi), Some(app)) =>
        Ok(Json.toJson(SmuIndividualResponse.make(
          indi.individualData,
          app.applicationData
        )))
      case _ => NoContent
