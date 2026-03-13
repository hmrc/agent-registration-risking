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

package uk.gov.hmrc.agentregistrationrisking.testsupport.testdata

import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.PayeRef
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistration.shared.Vrn
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement.Agreed
import uk.gov.hmrc.agentregistration.shared.individual.IndividualDateOfBirth.Provided
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.individual.IndividualVerifiedEmailAddress
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState.Finished
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName

trait TdIndividualProvidedDetails { dependencies: (TdBase) =>

  val individualProvidedDetails: IndividualProvidedDetails = IndividualProvidedDetails(
    _id = individualProvidedDetailsId,
    individualName = IndividualName(individualName),
    isPersonOfControl = true,
    internalUserId = Some(internalUserId),
    createdAt = instant,
    providedDetailsState = Finished,
    agentApplicationId = agentApplicationId,
    individualDateOfBirth = Some(Provided(individualDateOfBirth)),
    telephoneNumber = Some(TelephoneNumber(telephoneNumber)),
    emailAddress = Some(IndividualVerifiedEmailAddress(EmailAddress(email), true)),
    individualNino = Some(IndividualNino.Provided(nino)),
    individualSaUtr = Some(IndividualSaUtr.Provided(SaUtr(utr.value))),
    hmrcStandardForAgentsAgreed = Agreed,
    hasApprovedApplication = Some(true),
    vrns = Some(List(Vrn(vrn), Vrn(vrn))),
    payeRefs = Some(List(PayeRef(payeRef), PayeRef(payeRef)))
  )

}
