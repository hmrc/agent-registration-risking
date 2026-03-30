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

import org.scalatest.time.Seconds
import org.scalatest.time.Span
import uk.gov.hmrc.agentregistrationrisking.testsupport.UnitSpec
import uk.gov.hmrc.mongo.lock.MongoLockRepository
import uk.gov.hmrc.mongo.test.MongoSupport

import java.time.Clock
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Promise

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

  private def schedulerAt2359 = new Scheduler(clockAt(23, 59), mongoLockRepository)
  private val oneSecondAfterMidnight = LocalTime.parse("23:59").plusSeconds(1)

  "Scheduler" - {

    "execute a scheduled daily job" in {
      val scheduler = schedulerAt2359
      val executed = Promise[Unit]()

      scheduler.scheduleDaily(
        "test-job",
        oneSecondAfterMidnight,
        () => { executed.trySuccess(()); Future.successful(()) }
      )

      executed.future.futureValue `shouldBe` ()
    }

    "re-schedule after execution" in {
      val scheduler = schedulerAt2359
      val secondExecution = Promise[Unit]()
      val counter = new AtomicInteger(0)

      scheduler.scheduleDaily(
        "repeating-job",
        oneSecondAfterMidnight,
        () =>
          if counter.incrementAndGet() >= 2 then { val _ = secondExecution.trySuccess(()) }
          Future.successful(())
      )

      secondExecution.future.futureValue `shouldBe` ()
      counter.get() should be >= 2
    }

    "not crash when job throws an exception" in {
      val scheduler = schedulerAt2359
      val schedulerSurvived = Promise[Unit]()
      val counter = new AtomicInteger(0)

      scheduler.scheduleDaily(
        "failing-job",
        oneSecondAfterMidnight,
        () =>
          if counter.incrementAndGet() == 1 then throw new RuntimeException("boom")
          schedulerSurvived.trySuccess(())
          Future.successful(())
      )

      schedulerSurvived.future.futureValue `shouldBe` ()
    }

    "recover and re-schedule when job returns a failed Future" in {
      val scheduler = schedulerAt2359
      val schedulerSurvived = Promise[Unit]()
      val counter = new AtomicInteger(0)

      scheduler.scheduleDaily(
        "async-failing-job",
        oneSecondAfterMidnight,
        () =>
          if counter.incrementAndGet() == 1 then Future.failed(new RuntimeException("async boom"))
          else { schedulerSurvived.trySuccess(()); Future.successful(()) }
      )

      schedulerSurvived.future.futureValue `shouldBe` ()
    }

    "only one instance runs the job when two schedulers compete for the same lock" in {
      val lockRepo = new MongoLockRepository(mongoComponent, new uk.gov.hmrc.mongo.CurrentTimestampSupport())
      val scheduler1 = new Scheduler(clockAt(23, 59), lockRepo)
      val scheduler2 = new Scheduler(clockAt(23, 59), lockRepo)

      val executionCount = new AtomicInteger(0)
      val firstJobStarted = Promise[Unit]()
      val holdLock = Promise[Unit]()

      scheduler1.scheduleDaily(
        "lock-contention-test",
        oneSecondAfterMidnight,
        () =>
          executionCount.incrementAndGet()
          firstJobStarted.trySuccess(())
          holdLock.future
      )

      // wait for first scheduler to acquire the lock
      firstJobStarted.future.futureValue `shouldBe` ()

      // second scheduler tries the same lock — should be skipped
      scheduler2.scheduleDaily(
        "lock-contention-test",
        oneSecondAfterMidnight,
        () =>
          executionCount.incrementAndGet()
          Future.successful(())
      )

      // give second scheduler time to attempt and be rejected
      eventually(timeout(Span(5, Seconds))) {
        executionCount.get() `shouldBe` 1
      }

      // clean up
      holdLock.success(())
    }
  }
