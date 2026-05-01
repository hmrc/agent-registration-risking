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

import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRiskingId
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRiskingId
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.*

class SubscriptionServiceSpec
extends ISpec:

  val service: SubscriptionService = app.injector.instanceOf[SubscriptionService]
  val repo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  val individualRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  private def makeApp(
    id: String,
    failures: Option[List[EntityFailure]],
    isSubscribed: Boolean = false
  ): ApplicationForRisking = tdAll.llpApplicationForRisking.copy(
    _id = ApplicationForRiskingId(id),
    failures = failures,
    isSubscribed = isSubscribed
  )

  private def makeIndividual(
    appId: ApplicationForRiskingId,
    id: String,
    failures: Option[List[IndividualFailure]]
  ): IndividualForRisking =
    val base = tdAll.readyForSubmissionIndividual(appId)
    base.copy(
      _id = IndividualForRiskingId(id),
      individualProvidedDetails = base.individualProvidedDetails.copy(personReference = PersonReference(id)),
      failures = failures
    )

  private def makeAppWithIndividuals(
    appId: String,
    entityFailures: Option[List[EntityFailure]],
    individualFailures: List[Option[List[IndividualFailure]]]
  ): ApplicationWithIndividuals =
    val app = makeApp(appId, entityFailures)
    val individuals = individualFailures.zipWithIndex.map: (failures, i) =>
      makeIndividual(
        app._id,
        s"$appId-ind-$i",
        failures
      )
    ApplicationWithIndividuals(app, individuals)

  "getAllUnsubscribedApplicationsWithIndividualsWithResults" - {

    "returns application when entity and all individuals have results" in {
      val app = makeApp("all-results", failures = Some(List.empty))
      val ind = makeIndividual(
        app._id,
        "all-results-ind",
        failures = Some(List.empty)
      )
      repo.upsert(app).futureValue
      individualRepo.upsert(ind).futureValue

      val result = service.findApprovedReadyToSubscribe.futureValue
      result.size shouldBe 1
      result.headOption.value.application._id shouldBe app._id
    }

    "does not return application when entity has no failures yet" in {
      val app = makeApp("no-entity-failures", failures = None)
      val ind = makeIndividual(
        app._id,
        "no-entity-ind",
        failures = Some(List.empty)
      )
      repo.upsert(app).futureValue
      individualRepo.upsert(ind).futureValue

      val result = service.findApprovedReadyToSubscribe.futureValue
      result.size shouldBe 0
    }

    "does not return application when individual has no failures yet" in {
      val app = makeApp("partial-results", failures = Some(List.empty))
      val ind = makeIndividual(
        app._id,
        "partial-ind",
        failures = None
      )
      repo.upsert(app).futureValue
      individualRepo.upsert(ind).futureValue

      val result = service.findApprovedReadyToSubscribe.futureValue
      result.size shouldBe 0
    }

    "does not return already subscribed application" in {
      val app = makeApp(
        "subscribed",
        failures = Some(List.empty),
        isSubscribed = true
      )
      val ind = makeIndividual(
        app._id,
        "subscribed-ind",
        failures = Some(List.empty)
      )
      repo.upsert(app).futureValue
      individualRepo.upsert(ind).futureValue

      val result = service.findApprovedReadyToSubscribe.futureValue
      result.size shouldBe 0
    }
  }

  "getApplicationsReadyForFailureEmailCheckWithIndividuals" - {

    "returns application when not subscribed, not yet emailed, results received for entity and all individuals" in {
      val app = makeApp(
        "fail-check-1",
        failures = Some(List(EntityFailure._3._2))
      )
      val ind = makeIndividual(
        app._id,
        "fail-check-1-ind",
        failures = Some(List.empty)
      )
      repo.upsert(app).futureValue
      individualRepo.upsert(ind).futureValue

      val result = service.getApplicationsReadyForFailureEmailCheckWithIndividuals.futureValue
      result.size shouldBe 1
      result.headOption.value.application._id shouldBe app._id
    }

    "does not return application when entity has no results yet" in {
      val app = makeApp("fail-check-no-entity", failures = None)
      val ind = makeIndividual(
        app._id,
        "fail-check-no-entity-ind",
        failures = Some(List.empty)
      )
      repo.upsert(app).futureValue
      individualRepo.upsert(ind).futureValue

      val result = service.getApplicationsReadyForFailureEmailCheckWithIndividuals.futureValue
      result.size shouldBe 0
    }

    "does not return application when an individual has no results yet" in {
      val app = makeApp(
        "fail-check-partial",
        failures = Some(List(EntityFailure._3._2))
      )
      val ind = makeIndividual(
        app._id,
        "fail-check-partial-ind",
        failures = None
      )
      repo.upsert(app).futureValue
      individualRepo.upsert(ind).futureValue

      val result = service.getApplicationsReadyForFailureEmailCheckWithIndividuals.futureValue
      result.size shouldBe 0
    }

    "does not return already subscribed application" in {
      val app = makeApp(
        "fail-check-subscribed",
        failures = Some(List.empty),
        isSubscribed = true
      )
      val ind = makeIndividual(
        app._id,
        "fail-check-subscribed-ind",
        failures = Some(List.empty)
      )
      repo.upsert(app).futureValue
      individualRepo.upsert(ind).futureValue

      val result = service.getApplicationsReadyForFailureEmailCheckWithIndividuals.futureValue
      result.size shouldBe 0
    }

    "does not return application that has already been emailed" in {
      val app = makeApp(
        "fail-check-emailed",
        failures = Some(List(EntityFailure._3._2))
      ).copy(isEmailSent = true)
      val ind = makeIndividual(
        app._id,
        "fail-check-emailed-ind",
        failures = Some(List.empty)
      )
      repo.upsert(app).futureValue
      individualRepo.upsert(ind).futureValue

      val result = service.getApplicationsReadyForFailureEmailCheckWithIndividuals.futureValue
      result.size shouldBe 0
    }
  }

  "getApprovedApplicationsWithIndividuals" - {

    "returns application when entity and all individuals approved" in {
      val appWithInds = makeAppWithIndividuals(
        appId = "approved",
        entityFailures = Some(List.empty),
        individualFailures = List(Some(List.empty))
      )
      val result = service.getApprovedApplicationsWithIndividuals(Seq(appWithInds))
      result.size shouldBe 1
    }

    "does not return when entity has fixable failures" in {
      val appWithInds = makeAppWithIndividuals(
        "entity-fixable",
        Some(List(EntityFailure._3._2)), // fixable
        List(Some(List.empty))
      )
      val result = service.getApprovedApplicationsWithIndividuals(Seq(appWithInds))
      result.size shouldBe 0
    }

    "does not return when entity has non-fixable failures" in {
      val appWithInds = makeAppWithIndividuals(
        "entity-nonfixable",
        Some(List(EntityFailure._8._1)), // non-fixable
        List(Some(List.empty))
      )
      val result = service.getApprovedApplicationsWithIndividuals(Seq(appWithInds))
      result.size shouldBe 0
    }

    "does not return when individual has fixable failures" in {
      val appWithInds = makeAppWithIndividuals(
        "ind-fixable",
        Some(List.empty),
        List(Some(List(IndividualFailure._4._1))) // fixable
      )
      val result = service.getApprovedApplicationsWithIndividuals(Seq(appWithInds))
      result.size shouldBe 0
    }

    "does not return when individual has non-fixable failures" in {
      val appWithInds = makeAppWithIndividuals(
        "ind-nonfixable",
        Some(List.empty),
        List(Some(List(IndividualFailure._9))) // NonFixable
      )
      val result = service.getApprovedApplicationsWithIndividuals(Seq(appWithInds))
      result.size shouldBe 0
    }
  }

  "getNonFixableApplicationsWithIndividuals" - {

    "returns application with non-fixable entity failure and no individuals" in {
      val appWithInds = makeAppWithIndividuals(
        "nf-entity",
        Some(List(EntityFailure._8._1)), // NonFixable
        List(Some(List.empty))
      )
      val result = service.getNonFixableApplicationsWithIndividuals(Seq(appWithInds))
      result.size shouldBe 1
      result.headOption.value.individuals shouldBe empty
    }

    "returns application with non-fixable individual and only that individual" in {
      val app = makeApp("nf-ind-only", Some(List.empty))
      val indApproved = makeIndividual(
        app._id,
        "nf-ind-ok",
        Some(List.empty)
      )
      val indNonFixable = makeIndividual(
        app._id,
        "nf-ind-bad",
        Some(List(IndividualFailure._9)) // NonFixable
      )
      val appWithInds = ApplicationWithIndividuals(app, Seq(indApproved, indNonFixable))

      val result = service.getNonFixableApplicationsWithIndividuals(Seq(appWithInds))
      result.size shouldBe 1
      result.headOption.value.individuals.size shouldBe 1
      result.headOption.value.individuals.headOption.value._id shouldBe indNonFixable._id
    }

    "returns application when both entity and individual are non-fixable" in {
      val app = makeApp("nf-both", Some(List(EntityFailure._8._4))) // NonFixable
      val indNonFixable = makeIndividual(
        app._id,
        "nf-both-ind",
        Some(List(IndividualFailure._9)) // NonFixable
      )
      val appWithInds = ApplicationWithIndividuals(app, Seq(indNonFixable))

      val result = service.getNonFixableApplicationsWithIndividuals(Seq(appWithInds))
      result.size shouldBe 1
      result.headOption.value.individuals.size shouldBe 1
    }

    "does not return application when all approved" in {
      val appWithInds = makeAppWithIndividuals(
        "nf-none",
        Some(List.empty),
        List(Some(List.empty))
      )
      val result = service.getNonFixableApplicationsWithIndividuals(Seq(appWithInds))
      result.size shouldBe 0
    }

    "does not return application when only fixable failures" in {
      val appWithInds = makeAppWithIndividuals(
        "nf-fixable-only",
        Some(List(EntityFailure._3._2)), // Fixable
        List(Some(List(IndividualFailure._4._1))) // Fixable
      )
      val result = service.getNonFixableApplicationsWithIndividuals(Seq(appWithInds))
      result.size shouldBe 0
    }
  }

  "getFixableApplicationsWithIndividuals" - {

    "returns application with fixable entity failure" in {
      val appWithInds = makeAppWithIndividuals(
        "fix-entity",
        Some(List(EntityFailure._3._2)),
        List(Some(List.empty))
      )
      val result = service.getFixableApplicationsWithIndividuals(Seq(appWithInds))
      result.size shouldBe 1
      result.headOption.value.individuals shouldBe empty
    }

    "returns application with fixable individual and only that individual" in {
      val app = makeApp("fix-ind-only", Some(List.empty))
      val indApproved = makeIndividual(
        app._id,
        "fix-ind-ok",
        Some(List.empty)
      )
      val indFixable = makeIndividual(
        app._id,
        "fix-ind-bad",
        Some(List(IndividualFailure._4._1))
      )
      val appWithInds = ApplicationWithIndividuals(app, Seq(indApproved, indFixable))

      val result = service.getFixableApplicationsWithIndividuals(Seq(appWithInds))
      result.size shouldBe 1
      result.headOption.value.individuals.size shouldBe 1
      result.headOption.value.individuals.headOption.value._id shouldBe indFixable._id
    }

    "does not return application when entity has non-fixable failures" in {
      val appWithInds = makeAppWithIndividuals(
        "fix-nonfixable-entity",
        Some(List(EntityFailure._8._1)),
        List(Some(List(IndividualFailure._4._1)))
      )
      val result = service.getFixableApplicationsWithIndividuals(Seq(appWithInds))
      result.size shouldBe 0
    }

    "does not return application when any individual has non-fixable failures" in {
      val app = makeApp("fix-nonfixable-ind", Some(List(EntityFailure._3._2)))
      val indFixable = makeIndividual(
        app._id,
        "fix-mix-ok",
        Some(List(IndividualFailure._4._1))
      )
      val indNonFixable = makeIndividual(
        app._id,
        "fix-mix-bad",
        Some(List(IndividualFailure._9))
      )
      val appWithInds = ApplicationWithIndividuals(app, Seq(indFixable, indNonFixable))

      val result = service.getFixableApplicationsWithIndividuals(Seq(appWithInds))
      result.size shouldBe 0
    }

    "does not return application when all approved" in {
      val appWithInds = makeAppWithIndividuals(
        "fix-approved",
        Some(List.empty),
        List(Some(List.empty))
      )
      val result = service.getFixableApplicationsWithIndividuals(Seq(appWithInds))
      result.size shouldBe 0
    }
  }
