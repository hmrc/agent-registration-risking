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

import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentCorrespondenceAddress
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.AgentDetailsData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicantContactDetailsData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicationData
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates

class ApplicationForRiskingFleSpec
extends ISpec:

  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "field-level-encryption.enabled" -> true
  )

  private lazy val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  private lazy val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  private val td: TdApplicationWithIndividuals = TdRiskingInstancesInStates.approved
  private val applicationForRisking: ApplicationForRisking = td.application

  private def rawDocumentFor(applicationForRisking: ApplicationForRisking): Document =
    mongoComponent.database
      .getCollection("application-for-risking")
      .find(Filters.eq("applicationReference", applicationForRisking.applicationReference.value))
      .first()
      .toFuture()
      .futureValue

  override def beforeEach(): Unit =
    super.beforeEach()
    applicationForRiskingRepo.collection.drop().toFuture().futureValue
    individualForRiskingRepo.collection.drop().toFuture().futureValue
    ()

  // This spec proves the repo wiring runs the ApplicationDataEncryption transform — including nested/optional paths — on every write.
  "with FLE enabled the applicationData PII is encrypted at rest" in:
    applicationForRiskingRepo.upsert(applicationForRisking).futureValue

    val rawJson: String = rawDocumentFor(applicationForRisking).toJson()
    val applicationData: ApplicationData = applicationForRisking.applicationData
    val applicantContactDetailsData: ApplicantContactDetailsData = applicationData.applicantContactDetails
    val agentDetailsData: AgentDetailsData = applicationData.agentDetails
    val agentCorrespondenceAddress: AgentCorrespondenceAddress = agentDetailsData.agentCorrespondenceAddress

    rawJson should not include applicationData.internalUserId.value withClue "internalUserId encrypted"
    rawJson should not include applicationData.groupId.value withClue "groupId encrypted"
    rawJson should not include applicationData.applicantCredentials.providerId withClue "providerId encrypted"
    rawJson should not include applicantContactDetailsData.applicantName.value withClue "applicantName encrypted"
    rawJson should not include applicantContactDetailsData.telephoneNumber.value withClue "applicant telephoneNumber encrypted"
    rawJson should not include applicantContactDetailsData.applicantEmailAddress.value withClue "applicant email encrypted"
    rawJson should not include applicationData.amlsDetails.amlsRegistrationNumber.value withClue "amlsRegistrationNumber encrypted"
    rawJson should not include applicationData.amlsDetails.amlsEvidence.value.fileName withClue "amls evidence fileName encrypted"
    rawJson should not include agentDetailsData.businessName.agentBusinessName withClue "agentBusinessName encrypted"
    rawJson should not include agentCorrespondenceAddress.addressLine1 withClue "agent correspondence addressLine1 encrypted"
    rawJson should not include agentCorrespondenceAddress.addressLine2.value withClue "agent correspondence addressLine2 encrypted"
    rawJson should not include agentCorrespondenceAddress.postalCode.value withClue "agent correspondence postalCode encrypted"

    rawJson should include(applicationForRisking.applicationReference.value) withClue "applicationReference stays plaintext (search key)"

  "with FLE enabled findById round-trips to plaintext" in:
    applicationForRiskingRepo.upsert(applicationForRisking).futureValue
    applicationForRiskingRepo.findById(applicationForRisking.applicationReference).futureValue.value shouldBe applicationForRisking

  "with FLE enabled the aggregation path returns decrypted application+individuals" in:
    applicationForRiskingRepo.upsert(applicationForRisking).futureValue
    individualForRiskingRepo.upsert(td.individual1).futureValue
    individualForRiskingRepo.upsert(td.individual2).futureValue

    applicationForRiskingRepo.findApplicationsAwaitingOverallOutcome().futureValue should contain only td.applicationWithIndividuals
