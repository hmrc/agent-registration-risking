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

package uk.gov.hmrc.agentregistration.initializer

import play.api.Configuration
import uk.gov.hmrc.agentregistrationrisking.runner.RiskingRunner
import uk.gov.hmrc.agentregistrationrisking.services.ObjectStoreService
import uk.gov.hmrc.agentregistrationrisking.services.RiskingFileService
import uk.gov.hmrc.agentregistrationrisking.testsupport.UnitSpec

import java.time.LocalTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RiskingTaskSpec extends UnitSpec {

  private val stubRunner: RiskingRunner = new RiskingRunner(
    objectStoreService = null,
    riskingFileService = null
  ) {
    override def run(): Future[Unit] = Future.successful(())
  }

  "RiskingTask" - {

    "be enabled when config says true" in {
      val config = Configuration("scheduler.risking.enabled" -> true, "scheduler.risking.time" -> "02:00")
      val task   = new RiskingTask(stubRunner, config)
      task.enabled shouldBe true
    }

    "be disabled when config says false" in {
      val config = Configuration("scheduler.risking.enabled" -> false, "scheduler.risking.time" -> "02:00")
      val task   = new RiskingTask(stubRunner, config)
      task.enabled shouldBe false
    }

    "be disabled when config is missing" in {
      val config = Configuration("scheduler.risking.time" -> "02:00")
      val task   = new RiskingTask(stubRunner, config)
      task.enabled shouldBe false
    }

    "read scheduled time from config" in {
      val config = Configuration("scheduler.risking.enabled" -> true, "scheduler.risking.time" -> "02:00")
      val task   = new RiskingTask(stubRunner, config)
      task.scheduledTime.time shouldBe LocalTime.parse("02:00")
    }

    "have repeat set to true" in {
      val config = Configuration("scheduler.risking.enabled" -> true, "scheduler.risking.time" -> "02:00")
      val task   = new RiskingTask(stubRunner, config)
      task.repeat shouldBe true
    }

    "delegate run to riskingRunner" in {
      val config = Configuration("scheduler.risking.enabled" -> true, "scheduler.risking.time" -> "02:00")
      val task   = new RiskingTask(stubRunner, config)
      task.run().futureValue shouldBe ()
    }
  }
}
