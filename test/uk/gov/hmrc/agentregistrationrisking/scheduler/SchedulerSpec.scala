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
import uk.gov.hmrc.mongo.lock.LockRepository
import uk.gov.hmrc.mongo.lock.LockService

import java.time.Clock
import java.util.concurrent.TimeoutException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration.*

class SchedulerSpec
extends UnitSpec:

  private def newScheduler: Scheduler =
    new Scheduler(
      Clock.systemUTC(),
      null
    )

  "withJobTimeout" - {

    "completes with the job's result when the job finishes before the timeout" in {
      val result = newScheduler.withJobTimeout(name = "quick-job", timeout = 5.seconds)(Future.successful(()))
      result.futureValue shouldBe ()
    }

    "propagates the job's own failure when the job fails before the timeout" in {
      val boom = new RuntimeException("boom")
      val result = newScheduler.withJobTimeout(name = "failing-job", timeout = 5.seconds)(Future.failed(boom))
      result.failed.futureValue shouldBe boom
    }

    "fails with a TimeoutException naming the job when the job hangs beyond the timeout" in {
      val neverCompleting = Promise[Unit]().future
      val result = newScheduler.withJobTimeout(name = "hanging-job", timeout = 100.millis)(neverCompleting)
      val ex = result.failed.futureValue
      ex shouldBe a[TimeoutException]
      ex.getMessage should include("hanging-job")
    }

    "keeps the timeout failure even if the job completes later" in {
      val slowJob = Promise[Unit]()
      val result = newScheduler.withJobTimeout(name = "slow-job", timeout = 100.millis)(slowJob.future)
      result.failed.futureValue shouldBe a[TimeoutException]
      slowJob.success(())
      result.failed.futureValue shouldBe a[TimeoutException] withClue "late job completion must not overwrite the timeout failure"
    }
  }

  "jobTimeoutFor" - {

    def lockServiceWithTtl(ttlValue: Duration): LockService =
      new LockService:
        override val lockId: String = "test"
        override val ttl: Duration = ttlValue
        override val lockRepository: LockRepository = null

    "returns the lock TTL minus 10 minutes when the TTL is comfortably above the buffer" in {
      newScheduler.jobTimeoutFor(lockServiceWithTtl(1.hour)) shouldBe 50.minutes
    }

    "throws IllegalStateException when the derived timeout would be zero" in {
      val ex = intercept[IllegalStateException] {
        newScheduler.jobTimeoutFor(lockServiceWithTtl(10.minutes))
      }
      ex.getMessage should include("greater than 10 minutes")
    }

    "throws IllegalStateException when the derived timeout would be negative" in {
      val ex = intercept[IllegalStateException] {
        newScheduler.jobTimeoutFor(lockServiceWithTtl(5.minutes))
      }
      ex.getMessage should include("greater than 10 minutes")
    }

    "throws IllegalStateException when the lock TTL is infinite" in {
      val ex = intercept[IllegalStateException] {
        newScheduler.jobTimeoutFor(lockServiceWithTtl(Duration.Inf))
      }
      ex.getMessage should include("finite")
    }
  }
