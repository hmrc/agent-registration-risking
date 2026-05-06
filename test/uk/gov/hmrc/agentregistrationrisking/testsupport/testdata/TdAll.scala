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
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileName
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

object TdRisking:

  def make(
    instant: Instant,
    agentApplication: AgentApplication,
    personReferencePrefix: String,
    riskingFileName: RiskingFileName
  ): TdRisking =
    val instantParam: Instant = instant
    val agentApplicationParam: AgentApplication = agentApplication
    val personReferencePrefixParam: String = personReferencePrefix
    val riskingFileNameParam: RiskingFileName = riskingFileName
    new TdRisking:
      override def instant: Instant = instantParam
      override def agentApplication: AgentApplication = agentApplicationParam
      override def personReferencePrefix: String = personReferencePrefixParam
      override def riskingFileName: RiskingFileName = riskingFileNameParam

trait TdRisking:

  def agentApplication: AgentApplication
  def personReferencePrefix: String
  def instant: Instant
  def riskingFileName: RiskingFileName

  def tdApplicationForRisking: TdApplicationForRisking = TdApplicationForRisking.make(
    instant = instant,
    riskingFileName = riskingFileName,
    agentApplication = agentApplication
  )

  def tdIndividualsForRisking: TdIndividualsForRisking = TdIndividualsForRisking.make(
    instantParam = instant,
    personReferencePrefixParam = personReferencePrefix,
    applicationReferenceParam = agentApplication.applicationReference
  )

  def submitForRiskingRequest: SubmitForRiskingRequest = SubmitForRiskingRequest(
    agentApplication = agentApplication,
    individuals = List(
      tdIndividualsForRisking.tdIndividualForRisking1.readyForSubmission.individualProvidedDetails,
      tdIndividualsForRisking.tdIndividualForRisking2.readyForSubmission.individualProvidedDetails
    )
  )

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
  TdRiskingRecords:

  val tdRisking: TdRisking = TdRisking.make(
    instant = Instant.parse("2059-11-25T16:33:51Z"),
    agentApplication =
      TdApplicationsFactory
        .make(ApplicationReference("APPGENPAR1"))
        .agentApplicationGeneralPartnership
        .afterDeclarationSubmitted,
    personReferencePrefix = "PREFGENP",
    riskingFileName = RiskingFileName("asa_risking_file_version1_0_4_20591125_163351.txt")
  )

  val tdRisking2: TdRisking = TdRisking.make(
    instant = Instant.parse("2059-11-26T16:33:51Z"),
    agentApplication =
      TdApplicationsFactory
        .make(ApplicationReference("APPSOLTRA1"))
        .agentApplicationSoleTrader
        .afterDeclarationSubmitted,
    personReferencePrefix = "PREFSOLT",
    riskingFileName = RiskingFileName("asa_risking_file_version1_0_4_20591126_163351.txt")
  )

  val tdRisking3: TdRisking = TdRisking.make(
    instant = Instant.parse("2059-11-27T16:33:51Z"),
    agentApplication =
      TdApplicationsFactory
        .make(ApplicationReference("APPLLPART1"))
        .agentApplicationLlp
        .afterDeclarationSubmitted,
    personReferencePrefix = "PREFLLPA",
    riskingFileName = RiskingFileName("asa_risking_file_version1_0_4_20591127_163351.txt")
  )

  val tdRisking4: TdRisking = TdRisking.make(
    instant = Instant.parse("2059-11-28T16:33:51Z"),
    agentApplication =
      TdApplicationsFactory
        .make(ApplicationReference("APPSCOPAR1"))
        .agentApplicationScottishPartnership
        .afterDeclarationSubmitted,
    personReferencePrefix = "PREFSCOP",
    riskingFileName = RiskingFileName("asa_risking_file_version1_0_4_20591128_163351.txt")
  )
