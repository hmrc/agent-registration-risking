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

import com.google.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.agentregistrationrisking.action.Actions
import uk.gov.hmrc.agentregistrationrisking.model.sdes.*

class SdesNotificationController @Inject() (
  cc: ControllerComponents,
  actions: Actions
)
extends BackendController(cc):

  def receiveSdesNotification: Action[SdesNotification] =
    Action
      .apply(parse.json[SdesNotification]):
        implicit request =>
          request.body match
            case n: FileReady =>
              logger.info(s"File ready notification received for ${n.filename} from SDES [${n.correlationID}]")
              Ok
            case n: FileReceived =>
              logger.info(s"File received notification received for ${n.filename} from SDES [${n.correlationID}]")
              Ok
            case n: FileProcessed =>
              logger.info(s"File processed notification received for ${n.filename} from SDES")
              Ok
            case n: FileProcessingFailure =>
              logger.warn(s"File processing failure notification received for ${n.filename} from SDES. " +
                s"Reason: ${n.failureReason}. Action Required: ${n.actionRequired}")
              Ok
