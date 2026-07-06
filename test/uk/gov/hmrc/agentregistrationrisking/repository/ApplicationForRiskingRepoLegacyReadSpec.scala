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

package uk.gov.hmrc.agentregistrationrisking.repository

import com.softwaremill.quicklens.modify
import org.mongodb.scala.SingleObservableFuture
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates

import java.time.Instant

class ApplicationForRiskingRepoLegacyReadSpec
extends ISpec:

  private lazy val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]

  override def beforeEach(): Unit =
    super.beforeEach()
    applicationForRiskingRepo.collection.drop().toFuture.futureValue
    ()

  "findById derives overallStatus.emailSentAt from entityRiskingResult.receivedAt when a persisted record has emailsProcessed=true but no emailSentAt (legacy compatibility)" in:
    val legacyApplicationForRisking: ApplicationForRisking = TdRiskingInstancesInStates
      .approvedAfterEmailSent
      .application
      .modify(_.overallStatus.emailSentAt).setTo(None)
    val entityReceivedAt: Instant = legacyApplicationForRisking.entityRiskingResult.value.receivedAt

    applicationForRiskingRepo.upsert(legacyApplicationForRisking).futureValue

    val readBack: ApplicationForRisking =
      applicationForRiskingRepo
        .findById(legacyApplicationForRisking.applicationReference)
        .futureValue
        .value

    readBack.overallStatus.emailSentAt shouldBe Some(entityReceivedAt)

  "findById returns the persisted emailSentAt unchanged for a new-shape record where emailSentAt is set at write time" in:
    val newShapeEmailSentAt: Instant = frozenInstant.plusSeconds(60)
    val newShapeApplicationForRisking: ApplicationForRisking = TdRiskingInstancesInStates
      .approvedAfterEmailSent
      .application
      .modify(_.overallStatus.emailSentAt).setTo(Some(newShapeEmailSentAt))

    applicationForRiskingRepo.upsert(newShapeApplicationForRisking).futureValue

    val readBack: ApplicationForRisking =
      applicationForRiskingRepo
        .findById(newShapeApplicationForRisking.applicationReference)
        .futureValue
        .value

    readBack.overallStatus.emailSentAt shouldBe Some(newShapeEmailSentAt) withClue
      "the derivation must not override a value that was written to the document — only synthesise when the field is missing"
