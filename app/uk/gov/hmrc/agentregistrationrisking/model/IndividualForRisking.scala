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

package uk.gov.hmrc.agentregistrationrisking.model

import play.api.libs.json.Json
import play.api.libs.json.OFormat
import play.api.libs.json.Reads
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.IndividualData
import uk.gov.hmrc.agentregistrationrisking.util.MinervaDateFormats.*

import java.time.Instant

final case class IndividualForRisking(
  personReference: PersonReference, // primary Key
  applicationReference: ApplicationReference, // foreign Key to ApplicationForRisking
  individualData: IndividualData,
  createdAt: Instant,
  lastUpdatedAt: Instant,
  individualRiskingResult: Option[IndividualRiskingResult],
  isEmailSent: Boolean,
  isResubmission: Boolean
)

object IndividualForRisking:
  given format: OFormat[IndividualForRisking] =
    final case class IndividualForRiskingLegacy(
      personReference: PersonReference, // primary Key
      applicationReference: ApplicationReference, // foreign Key to ApplicationForRisking
      individualData: IndividualData,
      createdAt: Instant,
      lastUpdatedAt: Instant,
      individualRiskingResult: Option[IndividualRiskingResult],
      isEmailSent: Boolean
    )
    val legacyReads = Json.reads[IndividualForRiskingLegacy].map(a =>
      IndividualForRisking(
        personReference = a.personReference,
        applicationReference = a.applicationReference,
        individualData = a.individualData,
        createdAt = a.createdAt,
        lastUpdatedAt = a.lastUpdatedAt,
        individualRiskingResult = a.individualRiskingResult,
        isEmailSent = a.isEmailSent,
        isResubmission = false
      )
    )
    val modernReads: Reads[IndividualForRisking] = Json.reads[IndividualForRisking]
    val writes = Json.writes[IndividualForRisking]
    OFormat(modernReads orElse legacyReads, writes)
