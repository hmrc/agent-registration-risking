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

package uk.gov.hmrc.agentregistrationrisking.runner

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.services.ApplicationOutcomeService
import uk.gov.hmrc.agentregistrationrisking.services.BackendNotificationService
import uk.gov.hmrc.agentregistrationrisking.services.EmailServiceForApprovedApplications
import uk.gov.hmrc.agentregistrationrisking.services.EmailServiceForFailedFixable
import uk.gov.hmrc.agentregistrationrisking.services.EmailServiceForFailedNonFixable
import uk.gov.hmrc.agentregistrationrisking.services.RiskingArchivalService
import uk.gov.hmrc.agentregistrationrisking.services.RiskingResultsService
import uk.gov.hmrc.agentregistrationrisking.services.SubscriptionService
import uk.gov.hmrc.agentregistrationrisking.testsupport.UnitSpec

import java.time.Clock
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RiskingResultsFileProcessingRunnerSpec
extends UnitSpec:

  private val allStages: List[String] = List(
    "processResultsFiles",
    "processOverallOutcomes",
    "processSubscriptions",
    "processEmailsForApproved",
    "processEmailsForFailedNonFixable",
    "processEmailsForFailedFixable",
    "processBackendNotifications",
    "processArchivals"
  )

  "run" - {

    "runs every stage when all stages succeed and the resulting Future succeeds" in {
      val fixture = new Fixture(failingStages = Set.empty)
      fixture.runner.run().futureValue shouldBe ()
      fixture.executedStages shouldBe allStages
    }

    "still runs all downstream stages when a mid-pipeline stage fails asynchronously" in {
      val fixture = new Fixture(failingStages = Set("processSubscriptions"))
      fixture.runner.run().futureValue shouldBe ()
      fixture.executedStages shouldBe allStages withClue "stages after the failing one must still run"
    }

    "succeeds (does not propagate failure to the scheduler) even when every stage fails" in {
      val fixture = new Fixture(failingStages = allStages.toSet)
      fixture.runner.run().futureValue shouldBe ()
      fixture.executedStages shouldBe allStages
    }
  }

  /** Builds a runner whose services are stubs recording invocation order. Stage stubs never touch their (null) dependencies. Wart.Null is excluded for tests.
    */
  private class Fixture(
    failingStages: Set[String]
  ):

    private val calls: AtomicReference[Vector[String]] = new AtomicReference(Vector.empty)

    def executedStages: List[String] = calls.get().toList

    private def stubStage(name: String): Future[Unit] =
      val _ = calls.updateAndGet(_ :+ name)
      if failingStages.contains(name) then Future.failed(new RuntimeException(s"$name failed"))
      else Future.successful(())

    private val riskingResultsService =
      new RiskingResultsService(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
      ):
        override def processResultsFiles()(using RequestHeader): Future[Unit] = stubStage("processResultsFiles")

    private val applicationOutcomeService =
      new ApplicationOutcomeService(
        null,
        null,
        null,
        null
      ):
        override def processOverallOutcomes()(using RequestHeader): Future[Unit] = stubStage("processOverallOutcomes")

    private val subscriptionService =
      new SubscriptionService(
        null,
        null,
        null,
        null,
        null
      ):
        override def processSubscriptions()(using RequestHeader): Future[Unit] = stubStage("processSubscriptions")

    private val emailServiceForApprovedApplications =
      new EmailServiceForApprovedApplications(
        null,
        null,
        null,
        null
      ):
        override def processEmails()(using RequestHeader): Future[Unit] = stubStage("processEmailsForApproved")

    private val emailServiceForFailedNonFixable =
      new EmailServiceForFailedNonFixable(
        null,
        null,
        null,
        null
      ):
        override def processEmails()(using RequestHeader): Future[Unit] = stubStage("processEmailsForFailedNonFixable")

    private val emailServiceForFailedFixable =
      new EmailServiceForFailedFixable(
        null,
        null,
        null,
        null,
        null
      ):
        override def processEmails()(using RequestHeader): Future[Unit] = stubStage("processEmailsForFailedFixable")

    private val backendNotificationService =
      new BackendNotificationService(null, null):
        override def processBackendNotifications()(using RequestHeader): Future[Unit] = stubStage("processBackendNotifications")

    private val riskingArchivalService =
      new RiskingArchivalService(
        null,
        null,
        null,
        null,
        null,
        null
      ):
        override def processArchivals()(using RequestHeader): Future[Unit] = stubStage("processArchivals")

    val runner: RiskingResultsFileProcessingRunner =
      given AppConfig = null
      given Clock = Clock.systemUTC()
      new RiskingResultsFileProcessingRunner(
        riskingResultsService = riskingResultsService,
        applicationOutcomeService = applicationOutcomeService,
        subscriptionService = subscriptionService,
        emailServiceForApprovedApplications = emailServiceForApprovedApplications,
        emailServiceForFailedNonFixable = emailServiceForFailedNonFixable,
        emailServiceForFailedFixable = emailServiceForFailedFixable,
        backendNotificationService = backendNotificationService,
        riskingArchivalService = riskingArchivalService
      )
