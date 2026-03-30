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

import uk.gov.hmrc.agentregistrationrisking.scheduler.DateTimeExtensions.*
import uk.gov.hmrc.agentregistrationrisking.testsupport.UnitSpec

import java.time.LocalDate
import java.time.ZoneId

import scala.concurrent.duration.*

class DateTimeExtensionsSpec
extends UnitSpec:

  private val zoneId = ZoneId.of("Europe/London")

  private val monday    = LocalDate.parse("2026-03-30").atStartOfDay(zoneId)
  private val friday    = LocalDate.parse("2026-04-03").atStartOfDay(zoneId)
  private val saturday  = LocalDate.parse("2026-04-04").atStartOfDay(zoneId)
  private val sunday    = LocalDate.parse("2026-04-05").atStartOfDay(zoneId)
  private val nextMonday = LocalDate.parse("2026-04-06").atStartOfDay(zoneId)

  "isWeekday" - {

    "return true for Monday" in {
      monday.isWeekday `shouldBe` true
    }

    "return false for Saturday" in {
      saturday.isWeekday `shouldBe` false
    }

    "return false for Sunday" in {
      sunday.isWeekday `shouldBe` false
    }
  }

  "isWeekend" - {

    "return false for Monday" in {
      monday.isWeekend `shouldBe` false
    }

    "return true for Saturday" in {
      saturday.isWeekend `shouldBe` true
    }

    "return true for Sunday" in {
      sunday.isWeekend `shouldBe` true
    }
  }

  "nextWeekday" - {

    "return Tuesday from Monday" in {
      monday.nextWeekday() `shouldBe` monday.plusDays(1)
    }

    "return Monday from Friday" in {
      friday.nextWeekday() `shouldBe` nextMonday
    }

    "return Monday from Saturday" in {
      saturday.nextWeekday() `shouldBe` nextMonday
    }
  }

  "plusWorkingDay" - {

    "return same day when adding 0 on a weekday" in {
      monday.plusWorkingDay(0) `shouldBe` monday
    }

    "skip weekend when adding 1 from Friday" in {
      friday.plusWorkingDay(1) `shouldBe` nextMonday
    }

    "skip weekend when adding 0 from Saturday" in {
      saturday.plusWorkingDay(0) `shouldBe` nextMonday
    }
  }

  "+ operator" - {

    "return same date for zero duration" in {
      (monday + 0.days) `shouldBe` monday
    }

    "add days" in {
      (monday + 3.days) `shouldBe` monday.plusDays(3)
    }
  }

  "- operator with duration" - {

    "return same date for zero duration" in {
      (monday - 0.days) `shouldBe` monday
    }

    "subtract days" in {
      (friday - 2.days) `shouldBe` friday.minusDays(2)
    }
  }

  "- operator with ZonedDateTime" - {

    "return positive duration" in {
      val duration = friday - monday
      duration.toDays `shouldBe` 4
    }

    "return negative duration when subtracted from earlier date" in {
      val duration = monday - friday
      duration.toDays `shouldBe` -4
    }
  }

  "String to LocalDate conversion" - {

    "convert basic ISO format (yyyyMMdd)" in {
      "20260330".asDate `shouldBe` LocalDate.parse("2026-03-30")
    }

    "convert ISO local date format (yyyy-MM-dd)" in {
      "2026-03-30".asDate `shouldBe` LocalDate.parse("2026-03-30")
    }
  }
