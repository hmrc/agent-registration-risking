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

object TdFailures:

  object entityFailures:

    val fixable1: EntityFailure.Fixable = EntityFailure._3._1
    val fixable2: EntityFailure.Fixable = EntityFailure._3._2

    val nonFixable1: EntityFailure.NonFixable = EntityFailure._8._1
    val nonFixable2: EntityFailure.NonFixable = EntityFailure._8._4

  object individualFailures:

    val fixable1: IndividualFailure.Fixable = IndividualFailure._4._1
    val fixable2: IndividualFailure.Fixable = IndividualFailure._4._3

    val nonFixable1: IndividualFailure.NonFixable = IndividualFailure._6
    val nonFixable2: IndividualFailure.NonFixable = IndividualFailure._7

trait TdFactory:

  def instant: Instant = TdAll.tdAll.nowAsInstant
  def riskingFileName: RiskingFileName

  object GeneralPartnership:

    private val applicationReference: ApplicationReference = ApplicationReference("APPGENPAR1")
    val agentApplication: AgentApplicationGeneralPartnership =
      TdDelegate
        .makeTdApplications(applicationReference)
        .agentApplicationGeneralPartnership
        .afterDeclarationSubmitted

    val individualProvidedDetails1: IndividualProvidedDetails =
      TdDelegate
        .makeTdIndividualProvidedDetails(applicationReference, PersonReference("PERGENPAR1"))
        .providedDetails
        .afterFinished

    val individualProvidedDetails2: IndividualProvidedDetails =
      TdDelegate
        .makeTdIndividualProvidedDetails(applicationReference, PersonReference("PERGENPAR2"))
        .providedDetails
        .afterFinished

    val submitForRiskingRequest: SubmitForRiskingRequest = SubmitForRiskingRequest(
      agentApplication = agentApplication,
      individuals = List(individualProvidedDetails1, individualProvidedDetails2)
    )

    val applicationSubmitted: ApplicationForRisking = ApplicationForRisking(
      applicationReference = applicationReference,
      riskingFileName = None,
      agentApplication = agentApplication,
      createdAt = instant,
      lastUpdatedAt = instant,
      failures = None,
      isSubscribed = false,
      isEmailSent = false
    )

    val applicationSentForRisking: ApplicationForRisking = applicationSubmitted
      .copy(
        riskingFileName = Some(riskingFileName),
        lastUpdatedAt = instant.plus(1, ChronoUnit.DAYS)
      )

    object receivedRiskingResults:

      val applicationApproved: ApplicationForRisking = applicationSentForRisking.copy(
        failures = Some(List.empty)
      )

      val applicationFailedFixable: ApplicationForRisking = applicationSentForRisking.copy(
        failures = Some(List(
          TdFailures.entityFailures.fixable1,
          TdFailures.entityFailures.fixable2
        ))
      )

      val applicationFailedNonFixable: ApplicationForRisking = applicationSentForRisking.copy(
        failures = Some(List(
          TdFailures.entityFailures.fixable2,
          TdFailures.entityFailures.nonFixable2
        ))
      )

trait TdApplications
extends shared.testdata.TdBase,
  shared.testdata.TdGrsBusinessDetails,
  shared.testdata.agentapplication.TdAgentApplicationGeneralPartnership,
  shared.testdata.agentapplication.TdAgentApplicationLimitedCompany,
  shared.testdata.agentapplication.TdAgentApplicationLimitedPartnership,
  shared.testdata.agentapplication.TdAgentApplicationLlp,
  shared.testdata.agentapplication.TdAgentApplicationScottishLimitedPartnership,
  shared.testdata.agentapplication.TdAgentApplicationScottishPartnership,
  shared.testdata.agentapplication.TdAgentApplicationSoleTrader,
  shared.testdata.agentapplication.TdAgentApplicationSoleTraderRepresentative

object TdDelegate:

  def makeTdApplications(applicationReference: ApplicationReference) =
    new TdApplications:
      val applicationReferenceParam: ApplicationReference = applicationReference
      override def applicationReference: ApplicationReference = applicationReferenceParam

  def makeTdIndividualProvidedDetails(
    applicationReference: ApplicationReference,
    personReference: PersonReference
  ): TdIndividualProvidedDetails & TdBase =
    val personReferenceParam = personReference
    val applicationReferenceParam = applicationReference

    new shared.testdata.providedetails.individual.TdIndividualProvidedDetails
      with shared.testdata.TdBase:
      override def personReference: PersonReference = personReferenceParam
      override def applicationReference: ApplicationReference = applicationReferenceParam

object TdAll:

  def apply(): TdAll = new TdAll {}

  val tdAll: TdAll = new TdAll {}

/** TestData (Td), All instances
  */
trait TdAll
extends shared.testdata.TdBase,
  shared.testdata.TdGrsBusinessDetails,
  shared.testdata.providedetails.individual.TdIndividualProvidedDetails,
  shared.testdata.agentapplication.TdAgentApplicationGeneralPartnership,
  shared.testdata.agentapplication.TdAgentApplicationLimitedCompany,
  shared.testdata.agentapplication.TdAgentApplicationLimitedPartnership,
  shared.testdata.agentapplication.TdAgentApplicationLlp,
  shared.testdata.agentapplication.TdAgentApplicationScottishLimitedPartnership,
  shared.testdata.agentapplication.TdAgentApplicationScottishPartnership,
  shared.testdata.agentapplication.TdAgentApplicationSoleTrader,
  shared.testdata.agentapplication.TdAgentApplicationSoleTraderRepresentative,
  TdRequest,
  TdObjectStore,
  sdes.TdSdesData,
  TdSdesProxy,
  TdRiskingRecords
