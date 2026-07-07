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
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRisking

class ApplicationForRiskingRepoFailedFixableReadinessSpec
extends ISpec:

  private lazy val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  private lazy val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  override def beforeEach(): Unit =
    super.beforeEach()
    applicationForRiskingRepo.collection.drop().toFuture.futureValue
    individualForRiskingRepo.collection.drop().toFuture.futureValue
    ()

  "findReadyToNotifyBackend picks up FailedFixable applications once emailSentAt is set — proves the predicate is outcome-agnostic, so when the FailedFixable email service ships the notify step just works" in:
    val tdRisking: TdRisking = TdRisking.make("FailedFixableReadinessSpec")

    val failedFixableAfterEmailSent: ApplicationForRisking =
      tdRisking
        .tdApplicationForRisking
        .receivedRiskingResults
        .failedFixableAfterEmailSent
    val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.failedFixable
    val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.approved

    applicationForRiskingRepo.upsert(failedFixableAfterEmailSent).futureValue
    individualForRiskingRepo.upsert(individual1).futureValue
    individualForRiskingRepo.upsert(individual2).futureValue

    val readyToNotify = applicationForRiskingRepo.findReadyToNotifyBackend().futureValue

    readyToNotify.map(_.application.applicationReference).toSet shouldBe Set(failedFixableAfterEmailSent.applicationReference) withClue
      "predicate must include a FailedFixable app whose emailSentAt has been set — outcome-agnostic gating is what lets the future FailedFixable email PR turn on without touching this predicate"
