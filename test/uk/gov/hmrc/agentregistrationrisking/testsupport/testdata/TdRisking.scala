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

import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.AmlsCode
import uk.gov.hmrc.agentregistration.shared.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistration.shared.CheckResult
import uk.gov.hmrc.agentregistration.shared.Crn
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.GroupId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistration.shared.PayeRef
import uk.gov.hmrc.agentregistration.shared.SafeId
import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistration.shared.Vrn
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentBusinessName
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentCorrespondenceAddress
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentEmailAddress
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentTelephoneNumber
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicationData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.AgentDetailsFe
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.AmlsDetailsFe
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.AmlsEvidenceFe
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicantContactDetailsFe
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.SubmitForRiskingRequest
import uk.gov.hmrc.agentregistration.shared.upload.FileUploadReference
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileName
import uk.gov.hmrc.auth.core.retrieve.Credentials

import java.time.Instant
import scala.util.Random

trait TdRisking:

  def applicationData: ApplicationData
  def personReferencePrefix: String
  def instant: Instant
  def riskingFileName: RiskingFileName

  def tdApplicationForRisking: TdApplicationForRisking = TdApplicationForRisking.make(
    instant = instant,
    riskingFileName = riskingFileName,
    applicationData = applicationData
  )

  def tdIndividualsForRisking: TdIndividualsForRisking = TdIndividualsForRisking.make(
    instantParam = instant,
    personReferencePrefixParam = personReferencePrefix,
    applicationReferenceParam = applicationData.applicationReference
  )

  def submitForRiskingRequest: SubmitForRiskingRequest = SubmitForRiskingRequest(
    agentApplication = applicationData,
    individuals = List(
      tdIndividualsForRisking.tdIndividualForRisking1.readyForSubmission.individualProvidedDetails,
      tdIndividualsForRisking.tdIndividualForRisking2.readyForSubmission.individualProvidedDetails
    )
  )

object TdRisking:

  def make(
    seed: String
  ): TdRisking =
    object t:
      val random: Random = new scala.util.Random(seed.hashCode)
      val instant: Instant = Instant.parse("2059-11-26T16:33:51Z").plusSeconds(random.nextInt(1000000))
      val applicationData: ApplicationData = TdApplicationData.make(seed)

    new TdRisking:
      override def instant: Instant = t.instant
      override def applicationData: ApplicationData = t.applicationData
      override def personReferencePrefix: String = s"PREF_$seed"
      override def riskingFileName: RiskingFileName = RiskingFileName.make(t.instant)
