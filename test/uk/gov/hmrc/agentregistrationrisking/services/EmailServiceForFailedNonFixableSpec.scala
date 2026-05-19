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

import com.github.tomakehurst.wiremock.client.WireMock as wm
import org.mongodb.scala.SingleObservableFuture
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.StubMaker
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.EmailStubs

class EmailServiceForFailedNonFixableSpec
extends ISpec:

  private val emailService: EmailServiceForFailedNonFixable = app.injector.instanceOf[EmailServiceForFailedNonFixable]
  private val applicationRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  private val individualRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  private given RequestHeader = tdAll.fakeBackendRequest

  override def beforeEach(): Unit =
    super.beforeEach()
    applicationRepo.collection.drop().toFuture.futureValue
    individualRepo.collection.drop().toFuture.futureValue
    ()

  private def stubEmailEndpoint(): Unit =
    StubMaker.make(
      httpMethod = StubMaker.HttpMethod.POST,
      urlPattern = wm.urlEqualTo("/hmrc/email"),
      responseStatus = 202
    )
    ()

  private def insert(td: TdApplicationWithIndividuals): Unit =
    applicationRepo.upsert(td.application).futureValue
    td.individuals.foreach(individualRepo.upsert(_).futureValue)

  "processEmails" - {

    "sends 1 applicant email and 1 individual email when only 1 of 3 individuals has a NonFixable failure" in:
      stubEmailEndpoint()
      insert(TdRiskingInstancesInStates.failedNonFixableAfterOutcomeWith3IndividualsOnly1Failing)

      emailService.processEmails().futureValue

      EmailStubs.verifySendEmail(count = 2)

    "sends 1 applicant email and 2 individual emails when 2 of 3 individuals have a NonFixable failure" in:
      stubEmailEndpoint()
      insert(TdRiskingInstancesInStates.failedNonFixableAfterOutcomeWith3IndividualsWith2Failing)

      emailService.processEmails().futureValue

      EmailStubs.verifySendEmail(count = 3)

    "sends only the applicant email when the entity failure is NonFixable but no individual has a NonFixable failure" in:
      stubEmailEndpoint()
      insert(TdRiskingInstancesInStates.failedNonFixableAfterOutcomeEntityOnlyNoIndividualNonFixable)

      emailService.processEmails().futureValue

      EmailStubs.verifySendEmail(count = 1)

    "sends no emails when the application has already been processed (emailsProcessed = true)" in:
      stubEmailEndpoint()
      insert(TdRiskingInstancesInStates.failedNonFixableAfterAllEmailsProcessed)

      emailService.processEmails().futureValue

      EmailStubs.verifySendEmail(count = 0)

    "sends only the remaining individual email when one individual was already emailed in a prior run" in:
      stubEmailEndpoint()
      insert(TdRiskingInstancesInStates.failedNonFixableAfter2EmailsSent)

      emailService.processEmails().futureValue

      EmailStubs.verifySendEmail(count = 1)

    "sends only the applicant email when SoleTrader and the only individual is the applicant" in:
      stubEmailEndpoint()
      insert(TdRiskingInstancesInStates.failedNonFixableAfterOutcomeSoleTraderApplicantIsIndividual)

      emailService.processEmails().futureValue

      EmailStubs.verifySendEmail(count = 1)
  }
