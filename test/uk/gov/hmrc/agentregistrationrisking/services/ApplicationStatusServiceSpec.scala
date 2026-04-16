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

package uk.gov.hmrc.agentregistrationrisking.services

import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationForRiskingStatus
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistration.shared.risking.PersonReference
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.RiskingResultRecord
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.*

class ApplicationStatusServiceSpec
extends ISpec:

  val service: ApplicationStatusService = app.injector.instanceOf[ApplicationStatusService]
  val repo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]

  "updateApplicationStatuses" - {

    given RequestHeader = FakeRequest()

    val appRef = ApplicationReference("ABC123456")
    val personRef = PersonReference("1234567890")
    val personRef2 = PersonReference("9876543210")

    def individualWith(
      ref: PersonReference = personRef,
      status: ApplicationForRiskingStatus = ApplicationForRiskingStatus.Approved,
      failures: Option[List[IndividualFailure]] = Some(List.empty)
    ) = tdAll.readyForSubmissionIndividual(Some(ref)).copy(
      status = status,
      failures = failures
    )

    def applicationWith(
      entityFailures: Option[List[EntityFailure]],
      individualStatus: ApplicationForRiskingStatus,
      individualFailures: Option[List[IndividualFailure]]
    ) = tdAll.llpApplicationForRisking.copy(
      applicationReference = appRef,
      status = ApplicationForRiskingStatus.SubmittedForRisking,
      failures = entityFailures,
      individuals = List(
        individualWith(
          ref = personRef,
          status = individualStatus,
          failures = individualFailures
        )
      )
    )

    def applicationWithMultipleIndividuals(
      entityFailures: Option[List[EntityFailure]],
      individuals: List[IndividualForRisking]
    ) = tdAll.llpApplicationForRisking.copy(
      applicationReference = appRef,
      status = ApplicationForRiskingStatus.SubmittedForRisking,
      failures = entityFailures,
      individuals = individuals
    )

    "updates application status to Approved when entity and all individuals are approved" in {
      repo.upsert(applicationWith(
        entityFailures = Some(List.empty),
        individualStatus = ApplicationForRiskingStatus.Approved,
        individualFailures = Some(List.empty)
      )).futureValue

      service.updateApplicationStatuses(List(passRecord1, passRecord2)).futureValue

      val updated = repo.findByApplicationReference(appRef).futureValue.value
      updated.status shouldBe ApplicationForRiskingStatus.Approved
    }

    "updates application status to FailedFixable when entity has fixable failures" in {
      repo.upsert(applicationWith(
        entityFailures = Some(List(EntityFailure._3._2)),
        individualStatus = ApplicationForRiskingStatus.Approved,
        individualFailures = Some(List.empty)
      )).futureValue

      service.updateApplicationStatuses(List(passRecord1, passRecord2)).futureValue

      val updated = repo.findByApplicationReference(appRef).futureValue.value
      updated.status shouldBe ApplicationForRiskingStatus.FailedFixable
    }

    "updates application status to FailedNonFixable when any status is FailedNonFixable" in {
      repo.upsert(applicationWith(
        entityFailures = Some(List.empty),
        individualStatus = ApplicationForRiskingStatus.FailedNonFixable,
        individualFailures = Some(List(IndividualFailure._9))
      )).futureValue

      service.updateApplicationStatuses(List(passRecord1, passRecord2)).futureValue

      val updated = repo.findByApplicationReference(appRef).futureValue.value
      updated.status shouldBe ApplicationForRiskingStatus.FailedNonFixable
    }

    "updates application status to FailedFixable when individual has fixable failures" in {
      repo.upsert(applicationWith(
        entityFailures = Some(List.empty),
        individualStatus = ApplicationForRiskingStatus.FailedFixable,
        individualFailures = Some(List(IndividualFailure._4._1))
      )).futureValue

      service.updateApplicationStatuses(List(passRecord1, passRecord2)).futureValue

      val updated = repo.findByApplicationReference(appRef).futureValue.value
      updated.status shouldBe ApplicationForRiskingStatus.FailedFixable
    }

    "updates application status to FailedNonFixable when individual has non-fixable failures even if entity is approved" in {
      repo.upsert(applicationWith(
        entityFailures = Some(List.empty),
        individualStatus = ApplicationForRiskingStatus.FailedNonFixable,
        individualFailures = Some(List(IndividualFailure._8._1))
      )).futureValue

      service.updateApplicationStatuses(List(passRecord1, passRecord2)).futureValue

      val updated = repo.findByApplicationReference(appRef).futureValue.value
      updated.status shouldBe ApplicationForRiskingStatus.FailedNonFixable
    }

    "does not update application status when entity failures are not yet available" in {
      repo.upsert(applicationWith(
        entityFailures = None,
        individualStatus = ApplicationForRiskingStatus.Approved,
        individualFailures = Some(List.empty)
      )).futureValue

      service.updateApplicationStatuses(List(passRecord1, passRecord2)).futureValue

      val updated = repo.findByApplicationReference(appRef).futureValue.value
      updated.status shouldBe ApplicationForRiskingStatus.SubmittedForRisking
    }

    "does not update application status when individual status is not yet completed" in {
      repo.upsert(applicationWith(
        entityFailures = Some(List.empty),
        individualStatus = ApplicationForRiskingStatus.ReadyForSubmission,
        individualFailures = None
      )).futureValue

      service.updateApplicationStatuses(List(passRecord1, passRecord2)).futureValue

      val updated = repo.findByApplicationReference(appRef).futureValue.value
      updated.status shouldBe ApplicationForRiskingStatus.SubmittedForRisking
    }

    val passRecord2ndIndividual = RiskingResultRecord(
      recordType = "Individual",
      applicationReference = None,
      failures = Some(List.empty),
      personReference = Some(personRef2)
    )

    "does not update application status when one of multiple individuals is not yet completed" in {
      repo.upsert(applicationWithMultipleIndividuals(
        entityFailures = Some(List.empty),
        individuals = List(
          individualWith(
            ref = personRef,
            status = ApplicationForRiskingStatus.Approved,
            failures = Some(List.empty)
          ),
          individualWith(
            ref = personRef2,
            status = ApplicationForRiskingStatus.ReadyForSubmission,
            failures = None
          )
        )
      )).futureValue

      service.updateApplicationStatuses(List(
        passRecord1,
        passRecord2,
        passRecord2ndIndividual
      )).futureValue

      val updated = repo.findByApplicationReference(appRef).futureValue.value
      updated.status shouldBe ApplicationForRiskingStatus.SubmittedForRisking
    }

    "updates application status when all multiple individuals are completed" in {
      repo.upsert(applicationWithMultipleIndividuals(
        entityFailures = Some(List.empty),
        individuals = List(
          individualWith(
            ref = personRef,
            status = ApplicationForRiskingStatus.Approved,
            failures = Some(List.empty)
          ),
          individualWith(
            ref = personRef2,
            status = ApplicationForRiskingStatus.Approved,
            failures = Some(List.empty)
          )
        )
      )).futureValue

      service.updateApplicationStatuses(List(
        passRecord1,
        passRecord2,
        passRecord2ndIndividual
      )).futureValue

      val updated = repo.findByApplicationReference(appRef).futureValue.value
      updated.status shouldBe ApplicationForRiskingStatus.Approved
    }

    "updates application status to FailedFixable when multiple individuals have mixed outcomes" in {
      repo.upsert(applicationWithMultipleIndividuals(
        entityFailures = Some(List.empty),
        individuals = List(
          individualWith(
            ref = personRef,
            status = ApplicationForRiskingStatus.Approved,
            failures = Some(List.empty)
          ),
          individualWith(
            ref = personRef2,
            status = ApplicationForRiskingStatus.FailedFixable,
            failures = Some(List(IndividualFailure._4._1))
          )
        )
      )).futureValue

      service.updateApplicationStatuses(List(
        passRecord1,
        passRecord2,
        passRecord2ndIndividual
      )).futureValue

      val updated = repo.findByApplicationReference(appRef).futureValue.value
      updated.status shouldBe ApplicationForRiskingStatus.FailedFixable
    }

    val nonExistentAppRef = ApplicationReference("NON_EXISTENT")
    val nonExistentPersonRef = PersonReference("0000000000")

    val entityRecordForNonExistentApp = RiskingResultRecord(
      recordType = "Entity",
      applicationReference = Some(nonExistentAppRef),
      failures = Some(List.empty),
      personReference = None
    )

    val individualRecordForNonExistentPerson = RiskingResultRecord(
      recordType = "Individual",
      applicationReference = None,
      failures = Some(List.empty),
      personReference = Some(nonExistentPersonRef)
    )

    "still updates existing application when another application reference is not found in repo" in {
      repo.upsert(applicationWith(
        entityFailures = Some(List.empty),
        individualStatus = ApplicationForRiskingStatus.Approved,
        individualFailures = Some(List.empty)
      )).futureValue

      service.updateApplicationStatuses(List(
        passRecord1,
        passRecord2,
        entityRecordForNonExistentApp
      )).futureValue

      val updated = repo.findByApplicationReference(appRef).futureValue.value
      updated.status shouldBe ApplicationForRiskingStatus.Approved
    }

    "still updates existing application when another person reference is not found in repo" in {
      repo.upsert(applicationWith(
        entityFailures = Some(List.empty),
        individualStatus = ApplicationForRiskingStatus.Approved,
        individualFailures = Some(List.empty)
      )).futureValue

      service.updateApplicationStatuses(List(
        passRecord1,
        passRecord2,
        individualRecordForNonExistentPerson
      )).futureValue

      val updated = repo.findByApplicationReference(appRef).futureValue.value
      updated.status shouldBe ApplicationForRiskingStatus.Approved
    }
  }
