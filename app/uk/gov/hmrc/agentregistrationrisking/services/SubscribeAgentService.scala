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
import uk.gov.hmrc.agentregistrationrisking.connectors.HipConnector
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.hip.Arn
import uk.gov.hmrc.agentregistrationrisking.model.hip.SubscribeAgentRequest
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class SubscribeAgentService @Inject() (
  hipConnector: HipConnector
)(using ExecutionContext)
extends RequestAwareLogging:

  def subscribeAgent(applicationForRisking: ApplicationForRisking)(using RequestHeader): Future[Arn] =
    val subscribeAgentRequest: SubscribeAgentRequest = SubscribeAgentRequest(
      name = applicationForRisking.agentDetails.businessName.getAgentBusinessName,
      addr1 = applicationForRisking.agentDetails.getAgentCorrespondenceAddress.addressLine1,
      addr2 = applicationForRisking.agentDetails.getAgentCorrespondenceAddress.addressLine2.getOrElse(""),
      addr3 = applicationForRisking.agentDetails.getAgentCorrespondenceAddress.addressLine3,
      addr4 = applicationForRisking.agentDetails.getAgentCorrespondenceAddress.addressLine4,
      postcode = applicationForRisking.agentDetails.getAgentCorrespondenceAddress.postalCode,
      country = applicationForRisking.agentDetails.getAgentCorrespondenceAddress.countryCode,
      phone = Some(applicationForRisking.agentDetails.getTelephoneNumber.agentTelephoneNumber),
      email = applicationForRisking.agentDetails.getAgentEmailAddress.getEmailAddress,
      supervisoryBody = Some(applicationForRisking.amlSupervisoryBody.value),
      membershipNumber = Some(applicationForRisking.amlRegNumber.value),
      evidenceObjectReference = None, // Evidence object reference is not required for agent subscription
      updateDetailsStatus = "ACCEPTED",
      amlSupervisionUpdateStatus = "ACCEPTED",
      directorPartnerUpdateStatus = "ACCEPTED",
      acceptNewTermsStatus = "ACCEPTED",
      reriskStatus = "ACCEPTED"
    )

    hipConnector.subscribeToAgentServices(
      safeId = applicationForRisking.entitySafeId,
      subscribeAgentRequest = subscribeAgentRequest
    )
