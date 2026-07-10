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
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Updates
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates

import java.time.Instant

class ApplicationForRiskingRepoLegacyReadSpec
extends ISpec:

  private lazy val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  private lazy val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  override def beforeEach(): Unit =
    super.beforeEach()
    applicationForRiskingRepo.collection.drop().toFuture.futureValue
    individualForRiskingRepo.collection.drop().toFuture.futureValue
    ()

  "findById derives overallStatus.emailsSentAt from entityRiskingResult.receivedAt when a persisted record has emailsProcessed=true but no emailSentAt (legacy compatibility)" in:
    val legacyApplicationForRisking: ApplicationForRisking = TdRiskingInstancesInStates
      .approvedAfterEmailSent
      .application
      .modify(_.overallStatus.emailsSentAt).setTo(None)
    val entityReceivedAt: Instant = legacyApplicationForRisking.entityRiskingResult.value.receivedAt

    applicationForRiskingRepo.upsert(legacyApplicationForRisking).futureValue

    val readBack: ApplicationForRisking =
      applicationForRiskingRepo
        .findById(legacyApplicationForRisking.applicationReference)
        .futureValue
        .value

    readBack.overallStatus.emailsSentAt shouldBe Some(entityReceivedAt)

  "findById returns the persisted emailSentAt unchanged for a new-shape record where emailSentAt is set at write time" in:
    val newShapeEmailSentAt: Instant = frozenInstant.plusSeconds(60)
    val newShapeApplicationForRisking: ApplicationForRisking = TdRiskingInstancesInStates
      .approvedAfterEmailSent
      .application
      .modify(_.overallStatus.emailsSentAt).setTo(Some(newShapeEmailSentAt))

    applicationForRiskingRepo.upsert(newShapeApplicationForRisking).futureValue

    val readBack: ApplicationForRisking =
      applicationForRiskingRepo
        .findById(newShapeApplicationForRisking.applicationReference)
        .futureValue
        .value

    readBack.overallStatus.emailsSentAt shouldBe Some(newShapeEmailSentAt) withClue
      "the derivation must not override a value that was written to the document — only synthesise when the field is missing"

  "findReadyToNotifyBackend picks up a legacy record where emailsProcessed=true but emailSentAt is missing on disk — proves legacy Approved/FailedNonFixable that were emailed under old flow (pre-emailSentAt) still get notified to BE" in:
    val legacyApp: ApplicationForRisking = TdRiskingInstancesInStates
      .approvedAfterEmailSent
      .application
      .modify(_.overallStatus.emailsSentAt).setTo(None)
    val individual1 = TdRiskingInstancesInStates.approvedAfterEmailSent.individual1
    val individual2 = TdRiskingInstancesInStates.approvedAfterEmailSent.individual2

    applicationForRiskingRepo.upsert(legacyApp).futureValue
    individualForRiskingRepo.upsert(individual1).futureValue
    individualForRiskingRepo.upsert(individual2).futureValue

    val ready: Seq[ApplicationWithIndividuals] = applicationForRiskingRepo.findReadyToNotifyBackend().futureValue

    ready.map(_.application.applicationReference).toSet shouldBe Set(legacyApp.applicationReference) withClue
      "predicate gate `emailsProcessed=true` must match legacy records that were emailed under the old flow before this PR added emailSentAt; the derivation on read fills emailSentAt = Some(receivedAt) so the wire builder gets a valid date"

    ready.head.application.overallStatus.emailsSentAt shouldBe Some(legacyApp.entityRiskingResult.value.receivedAt) withClue
      "after the derivation runs, the wire builder can safely call overallStatus.emailsSentAt.getOrThrowExpectedDataMissing"

  "findReadyToArchive does NOT match legacy records whose overallStatus.backendNotified field is absent from the persisted document — legacy pre-migration records that never had backendNotified flipped MUST NOT be archived (would erase evidence of an unfinished notify-BE flow)" in:
    val application: ApplicationForRisking = TdRiskingInstancesInStates.approvedAfterBackendNotified.application
    val individual1 = TdRiskingInstancesInStates.approvedAfterBackendNotified.individual1
    val individual2 = TdRiskingInstancesInStates.approvedAfterBackendNotified.individual2

    applicationForRiskingRepo.upsert(application).futureValue
    individualForRiskingRepo.upsert(individual1).futureValue
    individualForRiskingRepo.upsert(individual2).futureValue

    applicationForRiskingRepo.collection
      .updateOne(
        Filters.eq(FieldNames.applicationReference, application.applicationReference.value),
        Updates.unset(FieldNames.overallStatus.backendNotified)
      )
      .toFuture()
      .futureValue

    val ready: Seq[ApplicationWithIndividuals] = applicationForRiskingRepo.findReadyToArchive().futureValue

    ready.map(_.application.applicationReference).toSet should not contain application.applicationReference withClue
      "predicate gate `backendNotified=true` MUST NOT match legacy records missing the field on disk — those records represent an unfinished notify-BE flow and archiving them would silently erase evidence"
