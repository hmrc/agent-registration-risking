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
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.individual.IndividualVerifiedEmailAddress
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.risking.IndividualRiskingResponse
import uk.gov.hmrc.agentregistration.shared.risking.RiskingStatus
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRiskingId
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRiskingId

trait TdIndividualForRisking { dependencies: TdBase =>

  def readyForSubmissionIndividual(
    applicationForRiskingId: ApplicationForRiskingId = ApplicationForRiskingId("default-app-id")
  ): IndividualForRisking = IndividualForRisking(
    _id = IndividualForRiskingId(randomId()),
    applicationForRiskingId = applicationForRiskingId,
    individualProvidedDetails = IndividualProvidedDetails(
      _id = IndividualProvidedDetailsId(randomId()),
      personReference = dependencies.personReference,
      individualName = dependencies.individualName,
      isPersonOfControl = true,
      internalUserId = None,
      createdAt = dependencies.nowAsInstant,
      providedDetailsState = ProvidedDetailsState.Finished,
      agentApplicationId = dependencies.agentApplicationId,
      individualDateOfBirth = Some(IndividualDateOfBirth.Provided(dependencies.individualDateOfBirth)),
      telephoneNumber = Some(dependencies.telephoneNumber),
      emailAddress = Some(IndividualVerifiedEmailAddress(dependencies.individualEmailAddress, isVerified = true)),
      individualNino = Some(IndividualNino.Provided(dependencies.nino)),
      individualSaUtr = Some(dependencies.saUtrProvided),
      hmrcStandardForAgentsAgreed = StateOfAgreement.Agreed,
      hasApprovedApplication = Some(true),
      vrns = Some(List(dependencies.vrn, dependencies.vrn)),
      payeRefs = Some(List(dependencies.payeRef, dependencies.payeRef)),
      passedIv = Some(true)
    ),
    createdAt = dependencies.nowAsInstant,
    lastUpdatedAt = dependencies.nowAsInstant,
    failures = None
  )

  def individualRiskingResponseReadyForSubmission(
    personReference: PersonReference
  ) = IndividualRiskingResponse(
    personReference = personReference,
    providedName = dependencies.individualName,
    failures = None
  )

}
