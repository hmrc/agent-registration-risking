/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationrisking.testsupport.testdata

import uk.gov.hmrc.agentregistration.shared
import uk.gov.hmrc.agentregistration.shared
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistration.shared.risking.SubmitForRiskingRequest
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase
import uk.gov.hmrc.agentregistration.shared.testdata.providedetails.individual.TdIndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.TdAgentApplicationLlp
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileName
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.sdes.TdRiskingRecords

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit

//SubmitForRiskingRequest(app,individuals)
//x each business type
//AgentApplication + Individual1 + Individual2
//ApplicationReference + ApplicationId
//x2 IndividualProvidedDetailsId + PersonReference

//trait TdRisking:
//
//  val f =
//    new TdFactory:
//      override def instant: Instant = Instant.parse("2059-11-25T16:33:51Z")
//      override def riskingFileName: RiskingFileName = RiskingFileName("asa_risking_file_version1_0_4_20591125_163351.txt")
//
//  val f2 =
//    new TdFactory:
//      override def instant: Instant = Instant.parse("2059-11-26T16:33:51Z")
//      override def riskingFileName: RiskingFileName = RiskingFileName("asa_risking_file_version1_0_4_20591126_163351.txt")

object TdApplicationWithRiskingInstances:

  private object readyForSubmission:

    private val tdRisking: TdRisking = TdRiskingInstances.tdRisking
    val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.readyForSubmission
    val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.readyForSubmission
    val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.readyForSubmission

  private object submittedForRisking:

    private val tdRisking: TdRisking = TdRiskingInstances.tdRisking2
    val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.submittedForRisking
    val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.readyForSubmission
    val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.readyForSubmission
    val applicationWithIndividuals: ApplicationWithIndividuals = ApplicationWithIndividuals(
      application = application,
      individuals = Seq(individual1, individual2)
    )

  private object partiallyRisked:

    private val tdRisking: TdRisking = TdRiskingInstances.tdRisking6
    val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.submittedForRisking
    val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.submittedForRisking
    val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.failedFixable

    val applicationWithIndividuals: ApplicationWithIndividuals = ApplicationWithIndividuals(
      application = application,
      individuals = Seq(individual1, individual2)
    )

  private object approved:

    private val tdRisking: TdRisking = TdRiskingInstances.tdRisking3
    val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.receivedRiskingResults.approved
    val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.approved
    val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.approved
    val applicationWithIndividuals: ApplicationWithIndividuals = ApplicationWithIndividuals(
      application = application,
      individuals = Seq(individual1, individual2)
    )

  private object approvedAndSubscribed:

    private val tdRisking: TdRisking = TdRiskingInstances.tdRisking7
    val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.receivedRiskingResults.approvedAndSubscribed
    val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.approved
    val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.approved
    val applicationWithIndividuals: ApplicationWithIndividuals = ApplicationWithIndividuals(
      application = application,
      individuals = Seq(individual1, individual2)
    )

  private object approvedAndSubscribedAndEmailSent:

    private val tdRisking: TdRisking = TdRiskingInstances.tdRisking8
    val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.receivedRiskingResults.approvedAndSubscribedAndEmailSent
    val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.approved
    val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.approved
    val applicationWithIndividuals: ApplicationWithIndividuals = ApplicationWithIndividuals(
      application = application,
      individuals = Seq(individual1, individual2)
    )

  private object failedFixable:

    private val tdRisking: TdRisking = TdRiskingInstances.tdRisking4
    val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.receivedRiskingResults.approved
    val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.failedFixable
    val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.approved

  private object failedNonFixable:

    private val tdRisking: TdRisking = TdRiskingInstances.tdRisking5
    val application: ApplicationForRisking = tdRisking.tdApplicationForRisking.receivedRiskingResults.failedNonFixable
    val individual1: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking1.receivedRiskingResults.failedFixable
    val individual2: IndividualForRisking = tdRisking.tdIndividualsForRisking.tdIndividualForRisking2.receivedRiskingResults.approved

object TdAll:

  def apply(): TdAll = new TdAll {}

  val tdAll: TdAll = new TdAll {}

/** TestData (Td), All instances
  */
trait TdAll
extends TdRiskingBase,
  TdRequest,
  TdObjectStore,
  sdes.TdSdesData,
  TdSdesProxy,
  TdRiskingRecords,
  TdEmail:

  export TdRiskingInstances.*
