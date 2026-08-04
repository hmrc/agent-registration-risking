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
import java.time.ZonedDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import play.api.Logging
import uk.gov.hmrc.mongo.lock.LockRepository
import uk.gov.hmrc.mongo.lock.LockService
import uk.gov.hmrc.mongo.lock.MongoLockRepository

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration.*
import scala.util.Failure
import scala.util.Success
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig

@Singleton
class Scheduler @Inject() (
  clock: Clock,
  mongoLockRepository: MongoLockRepository
)(using ec: ExecutionContext)
extends Logging:

  private val executor = Executors.newScheduledThreadPool(1)

  private def now(): ZonedDateTime = ZonedDateTime.now(clock.withZone(AppConfig.zoneId))

  private def lockServiceFor(name: String): LockService =
    new LockService:
      override val lockId: String = s"schedules.$name"
      override val ttl: scala.concurrent.duration.Duration = 1.hour
      override val lockRepository: LockRepository = mongoLockRepository

  private[scheduler] def jobTimeoutFor(lockService: LockService): FiniteDuration =
    lockService.ttl match
      case finite: FiniteDuration =>
        val timeout = finite - 10.minutes
        if timeout <= Duration.Zero then
          throw new IllegalStateException(
            s"Lock TTL ($finite) must be greater than 10 minutes to leave a positive job timeout"
          )
        else timeout
      case _ => throw new IllegalStateException("Lock TTL must be a finite duration")

  private[scheduler] def withJobTimeout(
    name: String,
    timeout: FiniteDuration
  )(job: Future[Unit]): Future[Unit] =
    val promise = Promise[Unit]()
    val timeoutTask = executor.schedule(
      new Runnable:
        def run(): Unit =
          logger.warn(
            s"Scheduled task $name did not complete within ${timeout.toString}. The distributed lock will be released and the next tick may run concurrently with the underlying job, which cannot be cancelled and may still be in progress."
          )
          val _ = promise.tryFailure(new TimeoutException(s"Scheduled task $name timed out after ${timeout.toString}"))
      ,
      timeout.toMillis,
      TimeUnit.MILLISECONDS
    )
    job.onComplete { result =>
      val _ = timeoutTask.cancel(false)
      val _ = promise.tryComplete(result)
    }
    promise.future

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def scheduleDailyRiskingFileUpload(
    name: String,
    timeOfDay: LocalTime,
    job: () => Future[Unit]
  ): Unit =

    val nextRun: ZonedDateTime = nextDailyRunTime(timeOfDay)
    val delayMillis: Long = nextRun.toInstant.toEpochMilli - now().toInstant.toEpochMilli

    executor.schedule(
      new Runnable:
        def run(): Unit =
          val lockService = lockServiceFor(name)
          lockService.withLock {
            logger.info(s"Starting scheduled task: $name at ${ZonedDateTime.now(clock).toString}")
            withJobTimeout(name = name, timeout = jobTimeoutFor(lockService))(job())
          }.onComplete { result =>
            result match
              case Success(Some(_)) => logger.info(s"Scheduled task completed successfully: $name")
              case Success(None) => logger.debug(s"Scheduled task skipped - already running on another instance: $name")
              case Failure(e) => logger.error(s"Scheduled task failed: $name, ${e.getMessage}", e)
            scheduleDailyRiskingFileUpload(
              name = name,
              timeOfDay = timeOfDay,
              job = job
            )
          }
      ,
      delayMillis,
      TimeUnit.MILLISECONDS
    )
    logger.info(s"$name scheduled for ${nextRun.toString}")
    ()

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def scheduleHourlyResultsFileProcessing(
    name: String,
    job: () => Future[Unit]
  ): Unit =

    val nextRun: ZonedDateTime = nextHourlyRunTime()
    val delayMillis: Long = nextRun.toInstant.toEpochMilli - now().toInstant.toEpochMilli

    executor.schedule(
      new Runnable:
        def run(): Unit =
          val lockService = lockServiceFor(name)
          lockService.withLock {
            logger.info(s"Starting scheduled task: $name at ${ZonedDateTime.now(clock).toString}")
            withJobTimeout(name = name, timeout = jobTimeoutFor(lockService))(job())
          }.onComplete { result =>
            result match
              case Success(Some(_)) => logger.info(s"Scheduled task completed successfully: $name")
              case Success(None) => logger.debug(s"Scheduled task skipped - already running on another instance: $name")
              case Failure(e) => logger.error(s"Scheduled task failed: $name, ${e.getMessage}", e)
            scheduleHourlyResultsFileProcessing(
              name = name,
              job = job
            )
          }
      ,
      delayMillis,
      TimeUnit.MILLISECONDS
    )
    logger.info(s"$name scheduled for ${nextRun.toString}")
    ()

  private def nextDailyRunTime(timeOfDay: LocalTime): ZonedDateTime =
    val currentTime: ZonedDateTime = now()
    val today: ZonedDateTime = currentTime.`with`(timeOfDay)
    if currentTime.isBefore(today) then
      today
    else
      today.plusDays(1)

  private def nextHourlyRunTime(): ZonedDateTime = ZonedDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
