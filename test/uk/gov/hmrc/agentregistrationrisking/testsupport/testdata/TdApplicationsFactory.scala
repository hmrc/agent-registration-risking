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

import uk.gov.hmrc.agentregistration.shared
import uk.gov.hmrc.agentregistration.shared.ApplicationReference

object TdApplicationsFactory:
  def make(applicationReference: ApplicationReference): TdApplications =
    new TdApplications:
      val applicationReferenceParam: ApplicationReference = applicationReference
      override def applicationReference: ApplicationReference = applicationReferenceParam

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
