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
import play.api.mvc.Action
import play.api.mvc.ControllerComponents
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.agentregistrationrisking.action.Actions
import uk.gov.hmrc.agentregistrationrisking.model.sdes.*
import uk.gov.hmrc.agentregistrationrisking.services.ResultsFileService

class SdesNotificationController @Inject() (
  cc: ControllerComponents,
  actions: Actions,
  resultsFileService: ResultsFileService
)(using ExecutionContext)
extends BackendController(cc):

  def receiveSdesNotification: Action[SdesNotification] =
    Action
      .async(parse.json[SdesNotification]):
        implicit request =>
          request.body match
            case n: FileReady =>
              logger.info(s"File ready notification received for ${n.filename} from SDES [${n.correlationID}]")
              resultsFileService.retrieveAndProcessResultsFiles.map(_ => Ok)
            case n: FileReceived =>
              logger.info(s"File received notification received for ${n.filename} from SDES [${n.correlationID}]")
              Future.successful(Ok)
            case n: FileProcessed =>
              logger.info(s"File processed notification received for ${n.filename} from SDES")
              Future.successful(Ok)
            case n: FileProcessingFailure =>
              logger.warn(s"File processing failure notification received for ${n.filename} from SDES. " +
                s"Reason: ${n.failureReason}. Action Required: ${n.actionRequired}")
              Future.successful(Ok)
