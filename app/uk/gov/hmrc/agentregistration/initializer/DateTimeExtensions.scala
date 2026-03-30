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

import java.time.*
import java.time.format.DateTimeFormatter

import scala.annotation.tailrec
import scala.concurrent.duration.*

object DateTimeExtensions:

  private val Weekend: Seq[DayOfWeek] = Seq(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

  extension (date: ZonedDateTime)

    def isWeekend: Boolean = Weekend.contains(date.getDayOfWeek)

    def isWeekday: Boolean = !isWeekend

    def nextWeekday(): ZonedDateTime =
      date.getDayOfWeek match
        case DayOfWeek.FRIDAY   => date.plusDays(3)
        case DayOfWeek.SATURDAY => date.plusDays(2)
        case _                  => date.plusDays(1)

    def isWorkingDay: Boolean = !isWeekend

    def plusWorkingDay(days: Int): ZonedDateTime =
      require(days > -1, "n must be <= 0")

      @tailrec
      def next(d: ZonedDateTime): ZonedDateTime =
        d match
          case d if d.isWorkingDay => d
          case d                   => next(d.plusDays(1))

      next(date.plusDays(days))

    def +(duration: FiniteDuration): ZonedDateTime =
      if duration.toDays == 0 then date
      else date.plusDays(duration.toDays)

    def -(duration: FiniteDuration): ZonedDateTime =
      if duration.toDays == 0 then date
      else date.minusDays(duration.toDays)

    def -(other: ZonedDateTime): FiniteDuration =
      (date.toInstant.toEpochMilli - other.toInstant.toEpochMilli).millis

  given Conversion[String, LocalDate] with
    def apply(str: String): LocalDate =
      str match
        case date if date.matches("[0-9]{8}")                   => LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE)
        case date if date.matches("\\d\\d\\d\\d-\\d\\d-\\d\\d") => LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)

  extension (str: String)
    def asDate: LocalDate = summon[Conversion[String, LocalDate]].apply(str)
