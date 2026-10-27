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
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.AgentDetailsData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.AmlsDetailsData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicationData
import uk.gov.hmrc.agentregistrationrisking.model.hip.SubscribeAgentRequest
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging

object SubscribeAgentRequestBuilder
extends RequestAwareLogging:

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  def buildSubscribeAgentRequest(applicationData: ApplicationData)(using RequestHeader): SubscribeAgentRequest =
    val agentDetails: AgentDetailsData = applicationData.agentDetails
    val amlsDetails: AmlsDetailsData = applicationData.amlsDetails
    SubscribeAgentRequest(
      name = ensureFieldLength(agentDetails.businessName.getAgentBusinessName, 40).asInstanceOf[String],
      addr1 = ensureFieldLength(agentDetails.agentCorrespondenceAddress.addressLine1, 35).asInstanceOf[String],
      addr2 = ensureFieldLength(agentDetails.agentCorrespondenceAddress.addressLine2.getOrElse(""), 35).asInstanceOf[String],
      addr3 = ensureFieldLength(agentDetails.agentCorrespondenceAddress.addressLine3, 35).asInstanceOf[Option[String]],
      addr4 = ensureFieldLength(agentDetails.agentCorrespondenceAddress.addressLine4, 35).asInstanceOf[Option[String]],
      postcode = ensureFieldLength(agentDetails.agentCorrespondenceAddress.postalCode, 10).asInstanceOf[Option[String]],
      country = ensureCountryCode(agentDetails.agentCorrespondenceAddress.countryCode),
      phone = ensureFieldLength(Some(agentDetails.telephoneNumber.getAgentTelephoneNumber), 24).asInstanceOf[Option[String]],
      email = ensureFieldLength(agentDetails.agentEmailAddress, 132).asInstanceOf[EmailAddress],
      supervisoryBody = Some(amlsDetails.supervisoryBody.value),
      membershipNumber = Some(amlsDetails.amlsRegistrationNumber.value),
      evidenceObjectReference = None,
      updateDetailsStatus = "ACCEPTED",
      amlSupervisionUpdateStatus = "ACCEPTED",
      directorPartnerUpdateStatus = "ACCEPTED",
      acceptNewTermsStatus = "ACCEPTED",
      reriskStatus = "ACCEPTED"
    )

  private def ensureCountryCode(country: String)(using RequestHeader): String =
    val gbCountries: Set[String] = Set(
      "GB",
      "GREAT BRITAIN",
      "BRITAIN",
      "UNITED KINGDOM",
      "UK",
      "ENGLAND",
      "SCOTLAND",
      "WALES",
      "NORTHERN IRELAND"
    )
    if gbCountries.contains(country.toUpperCase)
    then "GB"
    else
      logger.info(s"Non-UK country provided: $country. Attempting to use first two characters as country code.")
      country.take(2).toUpperCase // this may or may not be a valid country code and may still be rejected by the API, but we have no better option for non-UK countries and the API will return a clear error if the code is invalid as opposed to a country code too long error

  private def ensureFieldLength(
    field: String | Option[String] | EmailAddress,
    max: Int
  ): String | Option[String] | EmailAddress =
    field match {
      case str: String => str.take(max)
      case opt: Option[String] => opt.map(_.take(max))
      case email: EmailAddress => EmailAddress(email.value.take(max))
    }
