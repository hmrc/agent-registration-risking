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

import uk.gov.hmrc.agentregistrationrisking.testsupport.UnitSpec
import uk.gov.hmrc.mongo.lock.MongoLockRepository
import uk.gov.hmrc.mongo.test.MongoSupport

import java.time.Clock
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SchedulerSpec
extends UnitSpec,
  MongoSupport:

  private val zoneId = ZoneId.of("Europe/London")

  private lazy val mongoLockRepository = new MongoLockRepository(mongoComponent, new uk.gov.hmrc.mongo.CurrentTimestampSupport())

  private def clockAt(
    hour: Int,
    minute: Int
  ): Clock =
    val instant =
      java.time.LocalDate.now()
        .atTime(hour, minute)
        .atZone(zoneId)
        .toInstant
    Clock.fixed(instant, zoneId)

  "Scheduler" - {

    "execute a scheduled daily job" in {
      val clock = clockAt(23, 59)
      val scheduler = new Scheduler(clock, mongoLockRepository)
      val latch = new CountDownLatch(1)
      val counter = new AtomicInteger(0)

      val time = LocalTime.parse("23:59").plusSeconds(1)

      scheduler.scheduleDaily(
        "test-job",
        time,
        () => {
          counter.incrementAndGet()
          latch.countDown()
          Future.successful(())
        }
      )

      latch.await(5, TimeUnit.SECONDS) `shouldBe` true
      counter.get() `shouldBe` 1
    }

    "re-schedule after execution" in {
      val clock = clockAt(23, 59)
      val scheduler = new Scheduler(clock, mongoLockRepository)
      val latch = new CountDownLatch(2)
      val counter = new AtomicInteger(0)

      val time = LocalTime.parse("23:59").plusSeconds(1)

      scheduler.scheduleDaily(
        "repeating-job",
        time,
        () => {
          counter.incrementAndGet()
          latch.countDown()
          Future.successful(())
        }
      )

      latch.await(5, TimeUnit.SECONDS) `shouldBe` true
      counter.get() `shouldBe` 2
    }

    "not crash when job throws an exception" in {
      val clock = clockAt(23, 59)
      val scheduler = new Scheduler(clock, mongoLockRepository)
      val latch = new CountDownLatch(1)

      val time = LocalTime.parse("23:59").plusSeconds(1)

      scheduler.scheduleDaily(
        "failing-job",
        time,
        () => {
          latch.countDown()
          throw new RuntimeException("boom")
        }
      )

      latch.await(5, TimeUnit.SECONDS) `shouldBe` true
    }

    "only one instance runs the job when two schedulers compete for the same lock" in {
      import scala.concurrent.Promise

      val clock = clockAt(23, 59)
      val lockRepo = new MongoLockRepository(mongoComponent, new uk.gov.hmrc.mongo.CurrentTimestampSupport())
      val scheduler1 = new Scheduler(clock, lockRepo)
      val scheduler2 = new Scheduler(clock, lockRepo)

      val executionCount = new AtomicInteger(0)
      val firstJobStarted = new CountDownLatch(1)
      val secondSchedulerAttempted = new CountDownLatch(1)
      val promise = Promise[Unit]()

      val time = LocalTime.parse("23:59").plusSeconds(1)

      // first job: returns a Future that stays incomplete — holds the lock
      val slowJob: () => Future[Unit] =
        () =>
          executionCount.incrementAndGet()
          firstJobStarted.countDown()
          promise.future

      // second job: should never run if the lock works
      val secondJob: () => Future[Unit] =
        () =>
          executionCount.incrementAndGet()
          Future.successful(())

      scheduler1.scheduleDaily(
        "lock-contention-test",
        time,
        slowJob
      )

      // wait for the first scheduler to grab the lock
      firstJobStarted.await(10, TimeUnit.SECONDS) `shouldBe` true

      // now schedule the second with the same lock name
      scheduler2.scheduleDaily(
        "lock-contention-test",
        time,
        secondJob
      )

      // give the second scheduler enough time to fire and attempt the lock
      secondSchedulerAttempted.await(3, TimeUnit.SECONDS)

      // the second scheduler should have been skipped — only first incremented
      executionCount.get() `shouldBe` 1

      // clean up: complete the first job's future
      promise.success(())
    }
  }
