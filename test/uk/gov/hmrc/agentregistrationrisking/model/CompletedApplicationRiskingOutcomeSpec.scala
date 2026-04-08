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

import uk.gov.hmrc.agentregistration.shared.risking.ApplicationForRiskingStatus
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationForRiskingStatus.*
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationReference
import uk.gov.hmrc.agentregistrationrisking.testsupport.UnitSpec

class CompletedApplicationRiskingOutcomeSpec
extends UnitSpec:

  val appRef = ApplicationReference("TEST-REF")

  def outcome(entityStatus: CompletedStatus, individualStatuses: List[CompletedStatus]): CompletedStatus =
    CompletedApplicationRiskingOutcome(appRef, entityStatus, individualStatuses).applicationStatus

  "applicationStatus" - {

    "returns Approved when entity and all individuals have no failures" in {
      outcome(Approved, List(Approved)) shouldBe Approved
    }

    "returns Approved when entity and multiple individuals all approved" in {
      outcome(Approved, List(Approved, Approved, Approved)) shouldBe Approved
    }

    "returns FailedNonFixable when entity has non-fixable failures" in {
      outcome(FailedNonFixable, List(Approved)) shouldBe FailedNonFixable
    }

    "returns FailedNonFixable when entity has non-fixable failures and individuals have fixable failures" in {
      outcome(FailedNonFixable, List(FailedFixable)) shouldBe FailedNonFixable
    }

    "returns FailedNonFixable when any individual has non-fixable failures" in {
      outcome(Approved, List(Approved, FailedNonFixable)) shouldBe FailedNonFixable
    }

    "returns FailedNonFixable when both entity and individual have non-fixable failures" in {
      outcome(FailedNonFixable, List(FailedNonFixable)) shouldBe FailedNonFixable
    }

    "returns FailedFixable when entity has fixable failures and all individuals approved" in {
      outcome(FailedFixable, List(Approved)) shouldBe FailedFixable
    }

    "returns FailedFixable when entity approved and any individual has fixable failures" in {
      outcome(Approved, List(Approved, FailedFixable)) shouldBe FailedFixable
    }

    "returns FailedFixable when entity and individuals all have only fixable failures" in {
      outcome(FailedFixable, List(FailedFixable, FailedFixable)) shouldBe FailedFixable
    }

    "returns Approved when entity approved and no individuals" in {
      outcome(Approved, List.empty) shouldBe Approved
    }
  }
