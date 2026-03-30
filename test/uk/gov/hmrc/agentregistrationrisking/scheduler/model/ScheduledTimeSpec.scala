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

package uk.gov.hmrc.agentregistrationrisking.scheduler.model

import uk.gov.hmrc.agentregistrationrisking.scheduler.model.ScheduledTime
import uk.gov.hmrc.agentregistrationrisking.testsupport.UnitSpec

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ScheduledTimeSpec
extends UnitSpec {

  private val zoneId: ZoneId = ZoneId.of("Europe/London")

  private val monday = LocalDate.parse("2026-03-30").atStartOfDay(zoneId)
  private val tuesday = LocalDate.parse("2026-03-31").atStartOfDay(zoneId)
  private val saturday = LocalDate.parse("2026-04-04").atStartOfDay(zoneId)
  private val sunday = LocalDate.parse("2026-04-05").atStartOfDay(zoneId)
  private val nextDay = LocalDate.parse("2026-04-06").atStartOfDay(zoneId)

  private val _2_AM = LocalTime.parse("02:00")
  private val _10_AM = LocalTime.parse("10:00")

  "nextAfter" - {

    "return today's scheduled time when now is before the scheduled time" in {
      val now = monday
      val runtime = ScheduledTime(_2_AM).nextAfter(now)
      runtime shouldBe monday.`with`(_2_AM)
    }

    "return tomorrow's scheduled time when now is after the scheduled time" in {
      val now = monday.`with`(_10_AM)
      val runtime = ScheduledTime(_2_AM).nextAfter(now)
      runtime shouldBe tuesday.`with`(_2_AM)
    }

    "return tomorrow's scheduled time when now is exactly the scheduled time" in {
      val now = monday.`with`(_2_AM)
      val runtime = ScheduledTime(_2_AM).nextAfter(now)
      runtime shouldBe tuesday.`with`(_2_AM)
    }

    "schedule on Saturday" in {
      val now = saturday
      val runtime = ScheduledTime(_2_AM).nextAfter(now)
      runtime shouldBe saturday.`with`(_2_AM)
    }

    "schedule on Sunday" in {
      val now = sunday
      val runtime = ScheduledTime(_2_AM).nextAfter(now)
      runtime shouldBe sunday.`with`(_2_AM)
    }

    "return next day when now is after the scheduled time on Sunday" in {
      val now = sunday.`with`(_10_AM)
      val runtime = ScheduledTime(_2_AM).nextAfter(now)
      runtime shouldBe nextDay.`with`(_2_AM)
    }
  }

  "timeUntilNext" - {

    "return positive duration when scheduled time is ahead" in {
      val now = monday
      val duration = ScheduledTime(_2_AM).timeUntilNext(now)
      duration.toHours shouldBe 2
    }

    "return duration until next day when scheduled time has passed" in {
      val now = monday.`with`(_10_AM)
      val duration = ScheduledTime(_2_AM).timeUntilNext(now)
      duration.toHours shouldBe 16
    }
  }

}
