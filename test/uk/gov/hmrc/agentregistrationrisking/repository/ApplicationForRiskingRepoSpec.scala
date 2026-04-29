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

package uk.gov.hmrc.agentregistrationrisking.repository

import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRiskingId
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileId
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.*

class ApplicationForRiskingRepoSpec
extends ISpec:

  val repo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]

  "findByApplicationReference" - {

    "returns application when found" in {
      val app = tdAll.llpApplicationForRisking.copy(_id = ApplicationForRiskingId("find-ref-1"))
      repo.upsert(app).futureValue

      val result = repo.findByApplicationReference(app.agentApplication.applicationReference).futureValue
      result.value._id shouldBe app._id
    }

    "returns None when not found" in {
      val result = repo.findByApplicationReference(uk.gov.hmrc.agentregistration.shared.ApplicationReference("NON_EXISTENT")).futureValue
      result shouldBe None
    }
  }

  "findReadyForSubmission" - {

    "returns applications without riskingFileId" in {
      val ready = tdAll.llpApplicationForRisking.copy(_id = ApplicationForRiskingId("ready-1"))
      val submitted = tdAll.llpApplicationForRisking.copy(
        _id = ApplicationForRiskingId("submitted-1"),
        riskingFileId = Some(RiskingFileId("file-1"))
      )
      repo.upsert(ready).futureValue
      repo.upsert(submitted).futureValue

      val result = repo.findReadyForSubmission().futureValue
      result.size shouldBe 1
      result.headOption.value._id shouldBe ready._id
    }
  }

  "findReadyForSubscription" - {

    "returns applications with empty failures and not subscribed" in {
      val ready = tdAll.llpApplicationForRisking.copy(
        _id = ApplicationForRiskingId("sub-ready-1"),
        failures = Some(List.empty)
      )
      repo.upsert(ready).futureValue

      val result = repo.findReadyForSubscription().futureValue
      result.size shouldBe 1
      result.headOption.value._id shouldBe ready._id
    }

    "does not return applications with non-empty failures" in {
      val failed = tdAll.llpApplicationForRisking.copy(
        _id = ApplicationForRiskingId("sub-failed-1"),
        failures = Some(List(EntityFailure._3._2))
      )
      repo.upsert(failed).futureValue

      val result = repo.findReadyForSubscription().futureValue
      result.size shouldBe 0
    }

    "does not return applications without failures" in {
      val noFailures = tdAll.llpApplicationForRisking.copy(
        _id = ApplicationForRiskingId("sub-none-1"),
        failures = None
      )
      repo.upsert(noFailures).futureValue

      val result = repo.findReadyForSubscription().futureValue
      result.size shouldBe 0
    }

    "does not return already subscribed applications" in {
      val subscribed = tdAll.llpApplicationForRisking.copy(
        _id = ApplicationForRiskingId("sub-done-1"),
        failures = Some(List.empty),
        isSubscribed = true
      )
      repo.upsert(subscribed).futureValue

      val result = repo.findReadyForSubscription().futureValue
      result.size shouldBe 0
    }
  }

  "findNotSubscribedWithResults" - {

    "returns applications with failures present and not subscribed" in {
      val withResults = tdAll.llpApplicationForRisking.copy(
        _id = ApplicationForRiskingId("results-1"),
        failures = Some(List(EntityFailure._3._2))
      )
      repo.upsert(withResults).futureValue

      val result = repo.findNotSubscribedWithResults().futureValue
      result.size shouldBe 1
      result.headOption.value._id shouldBe withResults._id
    }

    "returns applications with empty failures and not subscribed" in {
      val emptyFailures = tdAll.llpApplicationForRisking.copy(
        _id = ApplicationForRiskingId("results-empty-1"),
        failures = Some(List.empty)
      )
      repo.upsert(emptyFailures).futureValue

      val result = repo.findNotSubscribedWithResults().futureValue
      result.size shouldBe 1
    }

    "does not return applications without failures" in {
      val noFailures = tdAll.llpApplicationForRisking.copy(
        _id = ApplicationForRiskingId("results-none-1"),
        failures = None
      )
      repo.upsert(noFailures).futureValue

      val result = repo.findNotSubscribedWithResults().futureValue
      result.size shouldBe 0
    }

    "does not return already subscribed applications" in {
      val subscribed = tdAll.llpApplicationForRisking.copy(
        _id = ApplicationForRiskingId("results-sub-1"),
        failures = Some(List(EntityFailure._3._2)),
        isSubscribed = true
      )
      repo.upsert(subscribed).futureValue

      val result = repo.findNotSubscribedWithResults().futureValue
      result.size shouldBe 0
    }
  }

  "findSubscribedReadyForSuccessEmail" - {

    "returns applications that are subscribed and have not had a success email sent" in {
      val readyForEmail = tdAll.llpApplicationForRisking.copy(
        _id = ApplicationForRiskingId("email-ready-1"),
        isSubscribed = true,
        isEmailSent = false
      )
      repo.upsert(readyForEmail).futureValue

      val result = repo.findSubscribedReadyForSuccessEmail().futureValue
      result.size shouldBe 1
      result.headOption.value._id shouldBe readyForEmail._id
    }

    "does not return applications that are not subscribed" in {
      val notSubscribed = tdAll.llpApplicationForRisking.copy(
        _id = ApplicationForRiskingId("email-not-sub-1"),
        isSubscribed = false,
        isEmailSent = false
      )
      repo.upsert(notSubscribed).futureValue

      val result = repo.findSubscribedReadyForSuccessEmail().futureValue
      result.size shouldBe 0
    }

    "does not return applications whose success email has already been sent" in {
      val alreadyEmailed = tdAll.llpApplicationForRisking.copy(
        _id = ApplicationForRiskingId("email-sent-1"),
        isSubscribed = true,
        isEmailSent = true
      )
      repo.upsert(alreadyEmailed).futureValue

      val result = repo.findSubscribedReadyForSuccessEmail().futureValue
      result.size shouldBe 0
    }
  }

  "updateEmailSent" - {

    "marks the application's isEmailSent flag as true" in {
      val notEmailed = tdAll.llpApplicationForRisking.copy(
        _id = ApplicationForRiskingId("update-email-1"),
        isSubscribed = true,
        isEmailSent = false
      )
      repo.upsert(notEmailed).futureValue

      val updateResult = repo.updateEmailSent(notEmailed._id).futureValue
      updateResult.getModifiedCount shouldBe 1L

      repo.findSubscribedReadyForSuccessEmail().futureValue.size shouldBe 0
    }

    "is a no-op when the application id does not exist" in {
      val updateResult = repo.updateEmailSent(ApplicationForRiskingId("missing-id")).futureValue
      updateResult.getMatchedCount shouldBe 0L
    }
  }
