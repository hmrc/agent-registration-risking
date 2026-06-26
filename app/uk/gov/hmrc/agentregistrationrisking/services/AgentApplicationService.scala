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

package uk.gov.hmrc.agentregistrationrisking.services

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.risking.updates.UpdateApplicationStateSentToMinervaRequest
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.ApplicationReferenceGenerator
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.ApplicationState.SentToMinerva
import uk.gov.hmrc.agentregistrationrisking.connectors.AgentRegistrationConnector
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class AgentApplicationService @Inject() (
  agentRegistrationConnector: AgentRegistrationConnector
)(using ExecutionContext)
extends RequestAwareLogging:

  def updateApplicationStateSentToMinerva(
    applicationReferences: Seq[ApplicationReference]
  )(using request: RequestHeader): Future[Unit] =
    val updateApplicationStateSentToMinervaRequest = UpdateApplicationStateSentToMinervaRequest(
      applicationReferences = applicationReferences
    )
    agentRegistrationConnector.updateApplicationStatusSentForRisking(updateApplicationStateSentToMinervaRequest)
