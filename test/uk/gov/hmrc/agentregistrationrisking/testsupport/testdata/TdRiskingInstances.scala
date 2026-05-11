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

package uk.gov.hmrc.agentregistrationrisking.testsupport.testdata

import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileName

import java.time.Instant

object TdRiskingInstances:

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

  val tdRisking5: TdRisking = TdRisking.make(
    instant = Instant.parse("2059-11-29T16:33:51Z"),
    agentApplication =
      TdApplicationsFactory
        .make(ApplicationReference("APPLTDART1"))
        .agentApplicationLimitedCompany
        .afterDeclarationSubmitted,
    personReferencePrefix = "PREFLTDA",
    riskingFileName = RiskingFileName("asa_risking_file_version1_0_4_20591129_163351.txt")
  )

  val tdRisking6: TdRisking = TdRisking.make(
    instant = Instant.parse("2059-11-30T16:33:51Z"),
    agentApplication =
      TdApplicationsFactory
        .make(ApplicationReference("APPSOLTRR1"))
        .agentApplicationSoleTraderRepresentative
        .afterDeclarationSubmitted,
    personReferencePrefix = "PREFSOLR",
    riskingFileName = RiskingFileName("asa_risking_file_version1_0_4_20591130_163351.txt")
  )

  val tdRisking7: TdRisking = TdRisking.make(
    instant = Instant.parse("2059-12-01T16:33:51Z"),
    agentApplication =
      TdApplicationsFactory
        .make(ApplicationReference("APPSCOTLP1"))
        .agentApplicationScottishLimitedPartnership
        .afterDeclarationSubmitted,
    personReferencePrefix = "PREFSCLP",
    riskingFileName = RiskingFileName("asa_risking_file_version1_0_4_20591201_163351.txt")
  )

  val tdRisking8: TdRisking = TdRisking.make(
    instant = Instant.parse("2059-12-02T16:33:51Z"),
    agentApplication =
      TdApplicationsFactory
        .make(ApplicationReference("APPSCOTLP2"))
        .agentApplicationScottishLimitedPartnership
        .afterDeclarationSubmitted,
    personReferencePrefix = "PREFSCP2",
    riskingFileName = RiskingFileName("asa_risking_file_version1_0_4_20591202_163351.txt")
  )
