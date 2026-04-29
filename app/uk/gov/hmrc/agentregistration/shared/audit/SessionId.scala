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

<<<<<<<< HEAD:app/uk/gov/hmrc/agentregistration/shared/audit/SessionId.scala
package uk.gov.hmrc.agentregistration.shared.audit
========
package uk.gov.hmrc.agentregistrationrisking.model
>>>>>>>> e646367 ([APB-11100] fixes after sync from FE):app/uk/gov/hmrc/agentregistrationrisking/model/RiskingFileWithContent.scala

final case class RiskingFileWithContent(
  riskingFile: RiskingFile,
  riskingFileContent: RiskingFileWithContent.RiskingFileContent,
  numberOfRecords: RiskingFileWithContent.NumberOfRecords
)

<<<<<<<< HEAD:app/uk/gov/hmrc/agentregistration/shared/audit/SessionId.scala
final case class SessionId(value: String)

object SessionId:

  given format: Format[SessionId] = JsonFormatsFactory.makeValueClassFormat
========
object RiskingFileWithContent:

  type NumberOfRecords = Int
  type RiskingFileContent = String
>>>>>>>> e646367 ([APB-11100] fixes after sync from FE):app/uk/gov/hmrc/agentregistrationrisking/model/RiskingFileWithContent.scala
