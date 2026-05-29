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

/** Regression for the local-dev path where field-level encryption is turned off.
  *
  * In deployed environments the flag is always on; locally it is off so engineers can read Mongo documents as plaintext. The whole encrypt/decrypt pipeline
  * must therefore be a no-op when the flag is false.
  */
class ApplicationForRiskingFleDisabledSpec
extends ISpec:

  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "field-level-encryption.enabled" -> false
  )

  private lazy val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  private lazy val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  private val td: TdApplicationWithIndividuals = TdRiskingInstancesInStates.approved
  private val applicationForRisking: ApplicationForRisking = td.application

  private def rawDocumentFor(applicationForRisking: ApplicationForRisking): Document =
    mongoComponent.database
      .getCollection(applicationForRiskingRepo.collectionName)
      .find(Filters.eq("applicationReference", applicationForRisking.applicationReference.value))
      .first()
      .toFuture()
      .futureValue

  override def beforeEach(): Unit =
    super.beforeEach()
    applicationForRiskingRepo.collection.drop().toFuture().futureValue
    individualForRiskingRepo.collection.drop().toFuture().futureValue
    ()

  "with FLE disabled the applicationData PII is stored as plaintext" in:
    applicationForRiskingRepo.upsert(applicationForRisking).futureValue

    val rawJson: String = rawDocumentFor(applicationForRisking).toJson()
    val applicationData: ApplicationData = applicationForRisking.applicationData
    val applicantContactDetailsData: ApplicantContactDetailsData = applicationData.applicantContactDetails
    val agentDetailsData: AgentDetailsData = applicationData.agentDetails
    val agentCorrespondenceAddress: AgentCorrespondenceAddress = agentDetailsData.agentCorrespondenceAddress

    rawJson should include(applicationData.internalUserId.value) withClue "internalUserId plaintext"
    rawJson should include(applicationData.groupId.value) withClue "groupId plaintext"
    rawJson should include(applicationData.applicantCredentials.providerId) withClue "providerId plaintext"
    rawJson should include(applicantContactDetailsData.applicantName.value) withClue "applicantName plaintext"
    rawJson should include(applicantContactDetailsData.telephoneNumber.value) withClue "applicant telephoneNumber plaintext"
    rawJson should include(applicantContactDetailsData.applicantEmailAddress.value) withClue "applicant email plaintext"
    rawJson should include(applicationData.amlsDetails.amlsRegistrationNumber.value) withClue "amlsRegistrationNumber plaintext"
    rawJson should include(applicationData.amlsDetails.amlsEvidence.value.fileName) withClue "amls evidence fileName plaintext"
    rawJson should include(agentDetailsData.businessName.agentBusinessName) withClue "agentBusinessName plaintext"
    rawJson should include(agentCorrespondenceAddress.addressLine1) withClue "agent correspondence addressLine1 plaintext"

  "with FLE disabled findById returns the model unchanged" in:
    applicationForRiskingRepo.upsert(applicationForRisking).futureValue
    applicationForRiskingRepo.findById(applicationForRisking.applicationReference).futureValue.value shouldBe applicationForRisking

  "with FLE disabled the aggregation path returns the application+individuals unchanged" in:
    applicationForRiskingRepo.upsert(applicationForRisking).futureValue
    individualForRiskingRepo.upsert(td.individual1).futureValue
    individualForRiskingRepo.upsert(td.individual2).futureValue

    applicationForRiskingRepo.findApplicationsAwaitingOverallOutcome().futureValue should contain only td.applicationWithIndividuals
