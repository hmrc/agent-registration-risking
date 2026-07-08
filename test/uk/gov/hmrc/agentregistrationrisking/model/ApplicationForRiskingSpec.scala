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
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistrationrisking.testsupport.UnitSpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdInstant
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates

import java.time.Instant

class ApplicationForRiskingSpec
extends UnitSpec:

  "reads derives overallStatus.emailsSentAt from entityRiskingResult.receivedAt when a legacy document has emailsProcessed=true but no emailSentAt" in:
    val legacyApplicationForRisking: ApplicationForRisking = TdRiskingInstancesInStates
      .approvedAfterEmailSent
      .application
      .modify(_.overallStatus.emailsSentAt).setTo(None)
    val entityReceivedAt: Instant = legacyApplicationForRisking.entityRiskingResult.value.receivedAt

    val legacyJson = Json.toJson(legacyApplicationForRisking)
    val readBack: ApplicationForRisking = legacyJson.as[ApplicationForRisking]

    readBack.overallStatus.emailsSentAt shouldBe Some(entityReceivedAt)

  "reads does not overwrite an existing emailSentAt" in:
    val applicationForRisking: ApplicationForRisking = TdRiskingInstancesInStates.approvedAfterEmailSent.application
    val presetEmailSentAt: Instant = TdInstant.instant.plusSeconds(60)
    val applicationForRiskingWithEmailSentAt: ApplicationForRisking = applicationForRisking
      .modify(_.overallStatus.emailsSentAt).setTo(Some(presetEmailSentAt))

    val json = Json.toJson(applicationForRiskingWithEmailSentAt)
    val readBack: ApplicationForRisking = json.as[ApplicationForRisking]

    readBack.overallStatus.emailsSentAt shouldBe Some(presetEmailSentAt)

  "reads leaves emailSentAt as None when emailsProcessed=false" in:
    val applicationForRisking: ApplicationForRisking = TdRiskingInstancesInStates.approvedAfterOutcome.application

    val json = Json.toJson(applicationForRisking)
    val readBack: ApplicationForRisking = json.as[ApplicationForRisking]

    readBack.overallStatus.emailsProcessed shouldBe false
    readBack.overallStatus.emailsSentAt shouldBe None

  "reads leaves emailSentAt as None when emailsProcessed=true but entityRiskingResult is missing — impossible under the state ladder but the derivation must not crash on it" in:
    val impossibleApplicationForRisking: ApplicationForRisking = TdRiskingInstancesInStates
      .approvedAfterEmailSent
      .application
      .modify(_.overallStatus.emailsSentAt).setTo(None)
      .copy(entityRiskingResult = None)

    val json = Json.toJson(impossibleApplicationForRisking)
    val readBack: ApplicationForRisking = json.as[ApplicationForRisking]

    readBack.overallStatus.emailsProcessed shouldBe true
    readBack.overallStatus.emailsSentAt shouldBe None
