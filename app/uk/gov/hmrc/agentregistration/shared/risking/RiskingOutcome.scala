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

package uk.gov.hmrc.agentregistration.shared.risking

enum RiskingOutcome:

<<<<<<<< HEAD:app/uk/gov/hmrc/agentregistration/shared/risking/RiskingOutcome.scala
<<<<<<<< HEAD:app/uk/gov/hmrc/agentregistration/shared/risking/RiskingOutcome.scala
  case FailedNonFixable
  case FailedFixable
  case Approved
========
enum ApplicationForRiskingStatusOld:
========
enum RiskingStatus:
>>>>>>>> 232a634 ([WG][APB-11100] Do actions per Pav description):app/uk/gov/hmrc/agentregistration/shared/risking/RiskingStatus.scala

  case ReadyForSubmission
  case SubmittedForRisking
  case ReceivedRiskingResults

<<<<<<<< HEAD:app/uk/gov/hmrc/agentregistration/shared/risking/RiskingOutcome.scala
object ApplicationForRiskingStatusOld:

  given Format[ApplicationForRiskingStatusOld] = JsonFormatsFactory.makeEnumFormat[ApplicationForRiskingStatusOld]

  type RiskingCompletedStatus = Approved.type | FailedNonFixable.type | FailedFixable.type
>>>>>>>> 7c02966 ([APB-11100] model):app/uk/gov/hmrc/agentregistration/shared/risking/ApplicationForRiskingStatusOld.scala
========
object RiskingStatus:
  given Format[RiskingStatus] = JsonFormatsFactory.makeEnumFormat[RiskingStatus]
>>>>>>>> 232a634 ([WG][APB-11100] Do actions per Pav description):app/uk/gov/hmrc/agentregistration/shared/risking/RiskingStatus.scala
