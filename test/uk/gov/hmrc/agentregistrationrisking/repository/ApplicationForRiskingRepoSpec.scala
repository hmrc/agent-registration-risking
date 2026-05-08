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

import org.mongodb.scala.model.Filters
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRisking

class ApplicationForRiskingRepoSpec
extends ISpec:

  object readyForSubmission:

    private val tdRisking: TdRisking = tdAll.tdRisking
    val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.readyForSubmission
    val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.readyForSubmission
    val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.readyForSubmission

  object submittedForRisking:

    private val tdRisking: TdRisking = tdAll.tdRisking2
    val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.submittedForRisking
    val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.readyForSubmission
    val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.readyForSubmission
    val applicationWithIndividuals: ApplicationWithIndividuals = ApplicationWithIndividuals(
      application = application,
      individuals = Seq(individual1, individual2)
    )

  object partiallyRisked:

    private val tdRisking: TdRisking = tdAll.tdRisking6
    val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.submittedForRisking
    val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.submittedForRisking
    val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.approved

    val applicationWithIndividuals: ApplicationWithIndividuals = ApplicationWithIndividuals(
      application = application,
      individuals = Seq(individual1, individual2)
    )

  object approved:

    private val tdRisking: TdRisking = tdAll.tdRisking3
    val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.receivedRiskingResults.approved
    val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.approved
    val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.approved
    val applicationWithIndividuals: ApplicationWithIndividuals = ApplicationWithIndividuals(
      application = application,
      individuals = Seq(individual1, individual2)
    )

  object failedFixable:

    private val tdRisking: TdRisking = tdAll.tdRisking4
    val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.receivedRiskingResults.approved
    val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.failedFixable
    val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.approved

  object failedNonFixable:

    private val tdRisking: TdRisking = tdAll.tdRisking5
    val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.receivedRiskingResults.failedNonFixable
    val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.failedFixable
    val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.approved

  "getApplicationWithIndividualsSeq tests" in:
    val applicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
    val individualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

    // GIVEN
    applicationForRiskingRepo.collection.drop()

    applicationForRiskingRepo.upsert(readyForSubmission.application)
    individualForRiskingRepo.upsert(readyForSubmission.individual1)
    individualForRiskingRepo.upsert(readyForSubmission.individual2)

    applicationForRiskingRepo.upsert(submittedForRisking.application)
    individualForRiskingRepo.upsert(submittedForRisking.individual1)
    individualForRiskingRepo.upsert(submittedForRisking.individual2)

    applicationForRiskingRepo.upsert(partiallyRisked.application)
    individualForRiskingRepo.upsert(partiallyRisked.individual1)
    individualForRiskingRepo.upsert(partiallyRisked.individual2)

    applicationForRiskingRepo.upsert(approved.application)
    individualForRiskingRepo.upsert(approved.individual1)
    individualForRiskingRepo.upsert(approved.individual2)

    applicationForRiskingRepo.upsert(failedFixable.application)
    individualForRiskingRepo.upsert(failedFixable.individual1)
    individualForRiskingRepo.upsert(failedFixable.individual2)

    applicationForRiskingRepo.upsert(failedNonFixable.application)
    individualForRiskingRepo.upsert(failedNonFixable.individual1)
    individualForRiskingRepo.upsert(failedNonFixable.individual2)

    // THEN
    applicationForRiskingRepo.getApplicationWithIndividualsSeq(
      applicationFilter = Filters.eq(
        "applicationReference",
        approved.application.applicationReference.value
      ),
      individualElemMatchFilter = Filters.empty()
    ).futureValue shouldBe Seq(
      approved.applicationWithIndividuals
    )
