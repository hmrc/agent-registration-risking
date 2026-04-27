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

import javax.inject.Singleton

final case class RiskingFileId(value: String)

object RiskingFileId:

  given format: Format[RiskingFileId] = JsonFormatsFactory.makeValueClassFormat
  given pathBindable: PathBindable[RiskingFileId] = ValueClassBinder.valueClassBinder[RiskingFileId](_.value)

@Singleton
class RiskingFileIdGenerator:
  def nextRiskingFileId(): RiskingFileId = RiskingFileId(ObjectId.get().toHexString)
