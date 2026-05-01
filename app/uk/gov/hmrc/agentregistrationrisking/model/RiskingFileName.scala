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

package uk.gov.hmrc.agentregistrationrisking.model

import org.bson.types.ObjectId
import play.api.libs.json.Format
import play.api.mvc.PathBindable
import uk.gov.hmrc.agentregistration.shared.util.JsonFormatsFactory
import uk.gov.hmrc.agentregistration.shared.util.ValueClassBinder
import uk.gov.hmrc.agentregistrationrisking.util.MinervaDateFormats.asRiskingFileTimeStamp

import java.time.Instant
import javax.inject.Singleton

final case class RiskingFileName(value: String)

object RiskingFileName:

  def make(timestamp: Instant): RiskingFileName = RiskingFileName(s"asa_risking_file_version1_0_4_${timestamp.asRiskingFileTimeStamp}.txt")

  given format: Format[RiskingFileName] = JsonFormatsFactory.makeValueClassFormat
  given pathBindable: PathBindable[RiskingFileName] = ValueClassBinder.valueClassBinder[RiskingFileName](_.value)
