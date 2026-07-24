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

import com.softwaremill.quicklens.modify
import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicationData

import java.time.Instant

final case class ApplicationForRisking(
  applicationReference: ApplicationReference, // primary Key
  riskingFileName: Option[RiskingFileName], // foreign Key to RiskingFile
  applicationData: ApplicationData, // data submitted by agent-registration-frontend
  createdAt: Instant,
  lastUpdatedAt: Instant,
  entityRiskingResult: Option[EntityRiskingResult],
  isSubscribed: Boolean,
  isEmailSent: Boolean,
  overallStatus: OverallStatus,
  correctiveActionExpiryDate: Option[Instant],
  isResubmission: Boolean,
  entityAlreadyApproved: Boolean
)

object ApplicationForRisking:

  given format: OFormat[ApplicationForRisking] =
    val reads: Reads[ApplicationForRisking] =
      (
        (__ \ "applicationReference").read[ApplicationReference] and
          (__ \ "riskingFileName").readNullable[RiskingFileName] and
          (__ \ "applicationData").read[ApplicationData] and
          (__ \ "createdAt").read[Instant] and
          (__ \ "lastUpdatedAt").read[Instant] and
          (__ \ "entityRiskingResult").readNullable[EntityRiskingResult] and
          (__ \ "isSubscribed").read[Boolean] and
          (__ \ "isEmailSent").read[Boolean] and
          (__ \ "overallStatus").read[OverallStatus] and
          (__ \ "correctiveActionExpiryDate").readNullable[Instant] and
          (__ \ "isResubmission").readNullable[Boolean].map(_.getOrElse(false)) and
          (__ \ "entityAlreadyApproved").readNullable[Boolean].map(_.getOrElse(false))
      )(ApplicationForRisking.apply).map(deriveEmailSentAtFromLegacyRecord)
    val writes: OWrites[ApplicationForRisking] = Json.writes[ApplicationForRisking]
    OFormat(reads, writes)

  private def deriveEmailSentAtFromLegacyRecord(application: ApplicationForRisking): ApplicationForRisking =
    (application.overallStatus.emailsProcessed, application.overallStatus.emailsSentAt, application.entityRiskingResult) match
      case (true, None, Some(entityRiskingResult)) => application.modify(_.overallStatus.emailsSentAt).setTo(Some(entityRiskingResult.receivedAt))
      case _ => application
