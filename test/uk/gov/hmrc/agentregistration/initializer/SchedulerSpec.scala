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

import uk.gov.hmrc.agentregistration.initializer.model.{ScheduledTime, Task}
import uk.gov.hmrc.agentregistrationrisking.testsupport.UnitSpec

import java.time.{Clock, Instant, LocalTime, ZoneId}
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SchedulerSpec extends UnitSpec {

  private val zoneId = ZoneId.of("Europe/London")

  private def clockAt(hour: Int, minute: Int): Clock = {
    val instant = java.time.LocalDate.now()
      .atTime(hour, minute)
      .atZone(zoneId)
      .toInstant
    Clock.fixed(instant, zoneId)
  }

  private def taskThatCountsExecutions(
    time: LocalTime,
    shouldRepeat: Boolean,
    latch: CountDownLatch,
    counter: AtomicInteger = new AtomicInteger(0),
    isEnabled: Boolean = true
  ): Task[Unit] = new Task[Unit] {
    override def scheduledTime: ScheduledTime = ScheduledTime(time)
    override def repeat: Boolean              = shouldRepeat
    override def name: String                 = "test-task"
    override def enabled: Boolean             = isEnabled
    override def run(): Unit = {
      counter.incrementAndGet()
      latch.countDown()
    }
  }

  "Scheduler" - {

    "execute a scheduled task" in {
      val clock     = clockAt(23, 59)
      val scheduler = new Scheduler(clock)
      val latch     = new CountDownLatch(1)
      val counter   = new AtomicInteger(0)

      val time = LocalTime.parse("23:59").plusSeconds(1)
      val task = taskThatCountsExecutions(time, shouldRepeat = false, latch, counter)

      scheduler.schedule(task)

      latch.await(5, TimeUnit.SECONDS) shouldBe true
      counter.get() shouldBe 1
    }

    "not crash when task throws an exception" in {
      val clock     = clockAt(23, 59)
      val scheduler = new Scheduler(clock)
      val latch     = new CountDownLatch(1)

      val time = LocalTime.parse("23:59").plusSeconds(1)
      val task = new Task[Unit] {
        override def scheduledTime: ScheduledTime = ScheduledTime(time)
        override def repeat: Boolean              = false
        override def name: String                 = "failing-task"
        override def enabled: Boolean             = true
        override def run(): Unit = {
          latch.countDown()
          throw new RuntimeException("boom")
        }
      }

      scheduler.schedule(task)

      latch.await(5, TimeUnit.SECONDS) shouldBe true
    }
  }
}
