/*
 * Copyright 2025 HM Revenue & Customs
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

import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.amls.AmlsEvidence
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationForRiskingStatus
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationRiskingResponse
import uk.gov.hmrc.agentregistration.shared.risking.IndividualRiskingResponse
import uk.gov.hmrc.agentregistration.shared.risking.PersonReference
import uk.gov.hmrc.agentregistration.shared.upload.UploadId
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.objectstore.client.Path.File
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase

import java.time.Instant
import java.time.LocalDate

trait TdApplicationForRisking { dependencies: TdBase & TdIndividualForRisking =>

  private val createdAt: Instant = dependencies.nowAsInstant

  val llpApplicationForRisking: ApplicationForRisking = ApplicationForRisking(
    applicationReference = ApplicationReference(randomId),
    status = ApplicationForRiskingStatus.ReadyForSubmission,
    createdAt = createdAt,
    uploadedAt = None,
    fileName = None,
    applicantName = applicantName,
    applicantPhone = Some(telephoneNumber),
    applicantEmail = Some(applicantEmailAddress),
    entityType = BusinessType.Partnership.LimitedLiabilityPartnership,
    entityIdentifier = saUtr.asUtr,
    crn = Some(crn),
    vrns = s"${vrn.value},${vrn.value}",
    payeRefs = s"${payeRef.value},${payeRef.value}",
    amlSupervisoryBody = amlsCode,
    amlRegNumber = amlsRegistrationNumber,
    amlExpiryDate = Some(LocalDate.parse(dateString)),
    amlEvidence = Some(AmlsEvidence(
      UploadId("evidence-reference-123"),
      "certificate.pdf",
      File("test.txt")
    )),
    individuals = List(dependencies.readyForSubmissionIndividual(Some(this.personReference))),
    failures = None
  )

  def applicationRiskingResponseReadyForSubmission(
    applicationReference: ApplicationReference,
    personReference: PersonReference
  ) = ApplicationRiskingResponse(
    applicationReference = applicationReference,
    status = ApplicationForRiskingStatus.ReadyForSubmission,
    individuals = List(IndividualRiskingResponse(
      personReference = personReference,
      status = ApplicationForRiskingStatus.ReadyForSubmission,
      failures = None
    )),
    failures = None
  )

}
