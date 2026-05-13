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
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.SubmitForRiskingRequest
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
import java.time.ZoneId
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

  val tdRiskingInstancesInStates: TdRiskingInstancesInStates.type = TdRiskingInstancesInStates

object TdZoneId:
  val zoneId: ZoneId = ZoneId.of("UTC")
