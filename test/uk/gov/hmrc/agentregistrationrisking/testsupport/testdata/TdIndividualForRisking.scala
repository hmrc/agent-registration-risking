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

import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.individual.IndividualDateOfBirth
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.individual.IndividualDateOfBirth.Provided
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRiskingStatus
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.PersonReference

import java.time.Instant

trait TdIndividualForRisking { dependencies: TdBase =>

  private val createdAt: Instant = dependencies.instant

  val readyForSubmissionIndividual: IndividualForRisking = IndividualForRisking(
    personReference = PersonReference(individualProvidedDetailsId.value),
    status = ApplicationForRiskingStatus.ReadyForSubmission,
    vrns = s"$vrn,$vrn",
    payeRefs = s"$payeRef,$payeRef",
    companiesHouseName = None,
    companiesHouseDateOfBirth = None,
    providedName = IndividualName(individualName),
    providedDateOfBirth = Provided(individualDateOfBirth),
    nino = Some(IndividualNino.Provided(nino)),
    saUtr = Some(IndividualSaUtr.Provided(SaUtr(utr.value))),
    phoneNumber = TelephoneNumber(telephoneNumber),
    email = EmailAddress(email),
    providedByApplicant = true,
    passedIV = true,
    failures = None
  )

}
