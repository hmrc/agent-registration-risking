/*
 * Copyright 2024 HM Revenue & Customs
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

import java.time.{Clock, ZoneId, ZonedDateTime}
import java.util.concurrent.Executors

import uk.gov.hmrc.agentregistration.initializer.model.Task
import javax.inject.{Inject, Singleton}
import play.api.Logger

import scala.concurrent.duration.FiniteDuration

@Singleton
class Scheduler @Inject() (clock: Clock) {

  private val schedulerZoneId = ZoneId.of("Europe/London")
  private val logger: Logger  = Logger(this.getClass)

  private def now(): ZonedDateTime = ZonedDateTime.now(clock.withZone(schedulerZoneId))

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def schedule[A](task: Task[A]): Unit = {

    logger.info(s"${task.name} scheduled for ${task.scheduledTime.nextAfter(now()).toString}")

    val delay: FiniteDuration = task.scheduledTime.timeUntilNext(now())

    Executors
      .newScheduledThreadPool(1)
      .schedule(
        new Runnable {
          def run(): Unit = {
            logger.info(s"Executing scheduled task: ${task.name} at ${ZonedDateTime.now(clock).toString}")
            task.run()
            if (task.repeat) {
              schedule(task)
            }
            ()
          }
        },
        delay.length,
        delay.unit
      )
    ()
  }
}
