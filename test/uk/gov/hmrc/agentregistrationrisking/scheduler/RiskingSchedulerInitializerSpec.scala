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

package uk.gov.hmrc.agentregistrationrisking.scheduler

import play.api.Configuration
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.runner.RiskingRunner
import uk.gov.hmrc.agentregistrationrisking.scheduler.model.Task
import uk.gov.hmrc.agentregistrationrisking.testsupport.UnitSpec
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.Clock
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RiskingSchedulerInitializerSpec
extends UnitSpec:

  private val zoneId = ZoneId.of("Europe/London")
  private val clock = Clock.systemDefaultZone()

  private val stubRunner: RiskingRunner =
    new RiskingRunner(
      objectStoreService = null,
      riskingFileService = null
    ):
      override def run(): Future[Unit] = Future.successful(())

  private val baseConfig: Map[String, Any] = Map(
    "appName" -> "agent-registration-risking",
    "secure-data-exchange-proxy-config.information-type" -> "test",
    "secure-data-exchange-proxy-config.server-token" -> "test",
    "secure-data-exchange-proxy-config.srn" -> "test",
    "mongodb.application-for-risking-ttl" -> "1 day",
    "microservice.services.secure-data-exchange-proxy.host" -> "localhost",
    "microservice.services.secure-data-exchange-proxy.port" -> "8765"
  )

  private def appConfigWith(entries: (String, Any)*): AppConfig =
    val config = Configuration.from(baseConfig ++ entries.toMap)
    new AppConfig(new ServicesConfig(config), config)

  "RiskingSchedulerInitializer" - {

    "schedule the task when enabled" in {
      val scheduled = new AtomicBoolean(false)
      val stubScheduler = new Scheduler(clock):
        override def schedule[A](task: Task[A]): Unit = scheduled.set(true)

      val appConfig = appConfigWith("scheduler.risking.enabled" -> true, "scheduler.risking.time" -> "02:00")
      new RiskingSchedulerInitializer(stubRunner, stubScheduler, appConfig)

      scheduled.get() `shouldBe` true
    }

    "not schedule the task when disabled" in {
      val scheduled = new AtomicBoolean(false)
      val stubScheduler = new Scheduler(clock):
        override def schedule[A](task: Task[A]): Unit = scheduled.set(true)

      val appConfig = appConfigWith("scheduler.risking.enabled" -> false, "scheduler.risking.time" -> "02:00")
      new RiskingSchedulerInitializer(stubRunner, stubScheduler, appConfig)

      scheduled.get() `shouldBe` false
    }
  }
