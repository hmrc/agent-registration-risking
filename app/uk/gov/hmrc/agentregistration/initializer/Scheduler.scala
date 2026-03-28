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
import scala.util.{Failure, Success, Try}

@Singleton
class Scheduler @Inject() (clock: Clock) {

  private val schedulerZoneId = ZoneId.of("Europe/London")
  private val logger: Logger  = Logger(this.getClass)
  private val executor        = Executors.newScheduledThreadPool(1)

  private def now(): ZonedDateTime = ZonedDateTime.now(clock.withZone(schedulerZoneId))

  def schedule[A](task: Task[A]): Unit = {

    logger.info(s"${task.name} scheduled for ${task.scheduledTime.nextAfter(now()).toString}")

    val delay: FiniteDuration = task.scheduledTime.timeUntilNext(now())

    executor.schedule(
      new Runnable {
        def run(): Unit = {
          logger.info(s"Starting scheduled task: ${task.name} at ${ZonedDateTime.now(clock).toString}")
          Try(task.run()) match {
            case Success(_) =>
              logger.info(s"Scheduled task ${task.name} completed successfully")
            case Failure(e) =>
              logger.error(s"Scheduled task ${task.name} failed with exception: ${e.getMessage}", e)
          }
          if (task.repeat) {
            schedule(task)
          }
        }
      },
      delay.length,
      delay.unit
    )
    ()
  }
}
