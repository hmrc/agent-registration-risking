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

package uk.gov.hmrc.agentregistrationrisking.util

import uk.gov.hmrc.agentregistrationrisking.config.AppConfig

import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter.ofPattern

object MinervaDateFormats:

  extension (d: LocalDate)
    inline def asMinervaDate: String = d.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))

  extension (i: Instant)

    inline def asRiskingFileTimeStamp: String = riskingFileTimestampFormatter.format(i)
    inline def asMinervaHeaderDate: String = minervaHeaderFormatter.format(i)
    inline def asMinervaHeaderTime: String = minervaHeaderTimeFormatter.format(i)

  private val riskingFileTimestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(AppConfig.zoneId)
  private val minervaHeaderFormatter = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(AppConfig.zoneId)
  private val minervaHeaderTimeFormatter = DateTimeFormatter.ofPattern("HHmmss").withZone(AppConfig.zoneId)
