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

///*
// * Copyright 2026 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package uk.gov.hmrc.agentregistrationrisking.scheduler
//
//import play.api.Configuration
//import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
//import uk.gov.hmrc.agentregistrationrisking.runner.RiskingRunner
//import uk.gov.hmrc.agentregistrationrisking.testsupport.UnitSpec
//import uk.gov.hmrc.mongo.lock.MongoLockRepository
//import uk.gov.hmrc.mongo.test.MongoSupport
//import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
//
//import java.time.Clock
//import java.time.LocalTime
//import java.util.concurrent.atomic.AtomicBoolean
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future
//import java.util.concurrent.atomic.AtomicInteger
//import scala.concurrent.Promise
//import java.time.LocalDate
//import java.time.ZoneId
//import uk.gov.hmrc.mongo.CurrentTimestampSupport
//
//class RiskingSchedulerInitializerSpec
//extends UnitSpec,
//  MongoSupport:
//
//  private given clock: Clock = Clock.systemDefaultZone()
//  private lazy val mongoLockRepository = new MongoLockRepository(mongoComponent, CurrentTimestampSupport())
//
//  private val stubRunner: RiskingRunner =
//    new RiskingRunner(
//      objectStoreService = null,
//      riskingFileService = null,
//      sdesProxyService = null,
//      applicationForRiskingRepo = null,
//      individualForRiskingRepo = null,
//      riskingFileRepo = null,
//      riskingFileIdGenerator = null
//    ):
//      override def run(): Future[Unit] = Future.successful(())
//
//  private val baseConfig: Map[String, Any] = Map(
//    "appName" -> "agent-registration-risking",
//    "secure-data-exchange-proxy-config.information-type" -> "test",
//    "secure-data-exchange-proxy-config.server-token" -> "test",
//    "secure-data-exchange-proxy-config.srn" -> "test",
//    "mongodb.application-for-risking-ttl" -> "1 day",
//    "microservice.services.secure-data-exchange-proxy.host" -> "localhost",
//    "microservice.services.secure-data-exchange-proxy.port" -> "8765",
//    "microservice.services.hip.host" -> "localhost",
//    "microservice.services.hip.port" -> "9009",
//    "microservice.services.hip.authorization-token" -> "test-hip-auth-token",
//    "microservice.services.enrolment-store-proxy.host" -> "localhost",
//    "microservice.services.enrolment-store-proxy.port" -> "7775"
//  )
//
//  private def appConfigWith(entries: (String, Any)*): AppConfig =
//    val config = Configuration.from(baseConfig ++ entries.toMap)
//    new AppConfig(new ServicesConfig(config), config)
//
//  "RiskingSchedulerInitializer" - {
//
//    "schedule the task when enabled" in {
//      val scheduled = new AtomicBoolean(false)
//      val stubScheduler =
//        new Scheduler(clock, mongoLockRepository):
//          override def scheduleDaily(
//            name: String,
//            timeOfDay: LocalTime,
//            job: () => Future[Unit]
//          ): Unit = scheduled.set(true)
//
//      val appConfig = appConfigWith("scheduler.risking.enabled" -> true, "scheduler.risking.time" -> "02:00")
//      new RiskingSchedulerInitializer(
//        stubRunner,
//        stubScheduler,
//        appConfig
//      )
//
//      scheduled.get() `shouldBe` true
//    }
//
//    "not schedule the task when disabled" in {
//      val scheduled = new AtomicBoolean(false)
//      val stubScheduler =
//        new Scheduler(clock, mongoLockRepository):
//          override def scheduleDaily(
//            name: String,
//            timeOfDay: LocalTime,
//            job: () => Future[Unit]
//          ): Unit = scheduled.set(true)
//
//      val appConfig = appConfigWith("scheduler.risking.enabled" -> false, "scheduler.risking.time" -> "02:00")
//      new RiskingSchedulerInitializer(
//        stubRunner,
//        stubScheduler,
//        appConfig
//      )
//
//      scheduled.get() `shouldBe` false
//    }
//
//    "only one instance runs the job when two initializers compete for the same lock" in {
//      val executionCount = new AtomicInteger(0)
//      val firstJobStarted = Promise[Unit]()
//      val holdLock = Promise[Unit]()
//
//      // schedule for 02:00 with clock at 01:59:59 — fires in ~1 second
//      val appConfig = appConfigWith("scheduler.risking.enabled" -> true, "scheduler.risking.time" -> "02:00")
//      val zoneId = AppConfig.zoneId
//      val nearScheduleTime = Clock.fixed(
//        LocalDate.now().atTime(1, 59, 59).atZone(zoneId).toInstant,
//        zoneId
//      )
//
//      val lockRepo = new MongoLockRepository(mongoComponent, new CurrentTimestampSupport())
//      val scheduler1 = new Scheduler(nearScheduleTime, lockRepo)
//      val scheduler2 = new Scheduler(nearScheduleTime, lockRepo)
//
//      val slowRunner: RiskingRunner =
//        new RiskingRunner(
//          objectStoreService = null,
//          riskingFileService = null,
//          sdesProxyService = null,
//          applicationForRiskingRepo = null,
//          individualForRiskingRepo = null,
//          riskingFileRepo = null,
//          riskingFileIdGenerator = null
//        ):
//          override def run(): Future[Unit] =
//            executionCount.incrementAndGet()
//            firstJobStarted.trySuccess(())
//            holdLock.future
//
//      val fastRunner: RiskingRunner =
//        new RiskingRunner(
//          objectStoreService = null,
//          riskingFileService = null,
//          sdesProxyService = null,
//          applicationForRiskingRepo = null,
//          individualForRiskingRepo = null,
//          riskingFileRepo = null,
//          riskingFileIdGenerator = null
//        ):
//          override def run(): Future[Unit] =
//            executionCount.incrementAndGet()
//            Future.successful(())
//
//      new RiskingSchedulerInitializer(
//        slowRunner,
//        scheduler1,
//        appConfig
//      )
//      firstJobStarted.future.futureValue `shouldBe` ()
//
//      new RiskingSchedulerInitializer(
//        fastRunner,
//        scheduler2,
//        appConfig
//      )
//
//      eventually(timeout(org.scalatest.time.Span(5, org.scalatest.time.Seconds))) {
//        executionCount.get() `shouldBe` 1
//      }
//
//      holdLock.success(())
//    }
//  }
