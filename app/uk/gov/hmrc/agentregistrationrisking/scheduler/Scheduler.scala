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

import java.time.Clock
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import play.api.Logging
import uk.gov.hmrc.mongo.lock.LockRepository
import uk.gov.hmrc.mongo.lock.LockService
import uk.gov.hmrc.mongo.lock.MongoLockRepository

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.util.Failure
import scala.util.Success

@Singleton
class Scheduler @Inject() (
  clock: Clock,
  mongoLockRepository: MongoLockRepository
)(using ec: ExecutionContext)
extends Logging:

  private val schedulerZoneId = ZoneId.of("Europe/London")
  private val executor = Executors.newScheduledThreadPool(1)

  private def now(): ZonedDateTime = ZonedDateTime.now(clock.withZone(schedulerZoneId))

  def scheduleDaily(
    name: String,
    timeOfDay: LocalTime,
    job: () => Future[Unit]
  ): Unit =
    val lockService =
      new LockService:
        override val lockId: String = s"schedules.$name"
        override val ttl: scala.concurrent.duration.Duration = 1.hour
        override val lockRepository: LockRepository = mongoLockRepository

    scheduleNext(
      name,
      timeOfDay,
      job,
      lockService
    )

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def scheduleNext(
    name: String,
    timeOfDay: LocalTime,
    job: () => Future[Unit],
    lockService: LockService
  ): Unit =

    val nextRun = nextRunTime(timeOfDay)
    val delayMillis = nextRun.toInstant.toEpochMilli - now().toInstant.toEpochMilli

    logger.info(s"$name scheduled for ${nextRun.toString}")

    executor.schedule(
      new Runnable:
        def run(): Unit =
          logger.info(s"Starting scheduled task: $name at ${ZonedDateTime.now(clock).toString}")
          lockService.withLock(job()).onComplete {
            case Success(Some(_)) =>
              logger.info(s"Scheduled task $name completed successfully")
              scheduleNext(
                name,
                timeOfDay,
                job,
                lockService
              )
            case Success(None) =>
              logger.info(s"Scheduled task $name skipped - already running on another instance")
              scheduleNext(
                name,
                timeOfDay,
                job,
                lockService
              )
            case Failure(e) =>
              logger.error(s"Scheduled task $name failed with exception: ${e.getMessage}", e)
              scheduleNext(
                name,
                timeOfDay,
                job,
                lockService
              )
          }
      ,
      delayMillis,
      TimeUnit.MILLISECONDS
    )
    ()

  private def nextRunTime(timeOfDay: LocalTime): ZonedDateTime =
    val today = now().`with`(timeOfDay)
    if now().isBefore(today) then today
    else today.plusDays(1)
