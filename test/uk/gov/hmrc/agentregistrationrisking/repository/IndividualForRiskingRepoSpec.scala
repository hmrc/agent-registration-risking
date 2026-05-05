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

///*
// * Copyright 2026 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package uk.gov.hmrc.agentregistrationrisking.repository
//
//import uk.gov.hmrc.agentregistration.shared.PersonReference
//import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
//import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRiskingId
//import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRiskingId
//import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
//import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.*
//
//class IndividualForRiskingRepoSpec
//extends ISpec:
//
//  val repo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]
//
//  private def makeIndividual(
//    id: String,
//    appId: String,
//    personRef: String,
//    failures: Option[List[IndividualFailure]] = None
//  ) =
//    val base = tdAll.readyForSubmissionIndividual(ApplicationForRiskingId(appId))
//    base.copy(
//      _id = IndividualForRiskingId(id),
//      individualProvidedDetails = base.individualProvidedDetails.copy(personReference = PersonReference(personRef)),
//      failures = failures
//    )
//
//  "findByApplicationForRiskingId" - {
//
//    "returns all individuals for an application" in {
//      val ind1 = makeIndividual(
//        "ind-1",
//        "app-1",
//        "person-1"
//      )
//      val ind2 = makeIndividual(
//        "ind-2",
//        "app-1",
//        "person-2"
//      )
//      val ind3 = makeIndividual(
//        "ind-3",
//        "app-2",
//        "person-3"
//      )
//      repo.upsert(ind1).futureValue
//      repo.upsert(ind2).futureValue
//      repo.upsert(ind3).futureValue
//
//      val result = repo.findByApplicationForRiskingId(ApplicationForRiskingId("app-1")).futureValue
//      result.size shouldBe 2
//    }
//
//    "returns empty when no individuals found" in {
//      val result = repo.findByApplicationForRiskingId(ApplicationForRiskingId("non-existent")).futureValue
//      result shouldBe empty
//    }
//  }
//
//  "findByPersonReference" - {
//
//    "returns individual when found" in {
//      val ind = makeIndividual(
//        "find-ind-1",
//        "app-1",
//        "find-person-1"
//      )
//      repo.upsert(ind).futureValue
//
//      val result = repo.findByPersonReference(PersonReference("find-person-1")).futureValue
//      result.value._id shouldBe ind._id
//    }
//
//    "returns None when not found" in {
//      val result = repo.findByPersonReference(PersonReference("non-existent")).futureValue
//      result shouldBe None
//    }
//  }
