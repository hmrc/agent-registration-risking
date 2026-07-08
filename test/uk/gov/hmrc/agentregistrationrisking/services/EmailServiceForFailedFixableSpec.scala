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

import org.mongodb.scala.SingleObservableFuture
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.EmailStubs

class EmailServiceForFailedFixableSpec
extends ISpec:

  private val emailServiceForFailedFixable: EmailServiceForFailedFixable = app.injector.instanceOf[EmailServiceForFailedFixable]
  private val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  private val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  private given RequestHeader = tdAll.fakeBackendRequest

  override def beforeEach(): Unit =
    super.beforeEach()
    applicationForRiskingRepo.collection.drop().toFuture.futureValue
    individualForRiskingRepo.collection.drop().toFuture.futureValue
    ()

  "processEmails is a no-op when the features.fixable-failures flag is OFF (default in application.conf) — no Mongo query, no emails sent, application state unchanged" in:
    val td = TdRiskingInstancesInStates.failedFixableAfterOutcome
    applicationForRiskingRepo.upsert(td.application).futureValue
    individualForRiskingRepo.upsert(td.individual1).futureValue
    individualForRiskingRepo.upsert(td.individual2).futureValue

    emailServiceForFailedFixable.processEmails().futureValue

    EmailStubs.verifySendEmail(count = 0)
    val persistedApp: ApplicationForRisking = applicationForRiskingRepo.findById(td.application.applicationReference).futureValue.value
    persistedApp.isEmailSent shouldBe false
    persistedApp.overallStatus.emailsProcessed shouldBe false
    persistedApp.overallStatus.emailSentAt shouldBe None
