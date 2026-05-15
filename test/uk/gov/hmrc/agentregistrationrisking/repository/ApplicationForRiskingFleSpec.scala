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
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates

class ApplicationForRiskingFleSpec
extends ISpec:

  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "field-level-encryption.enabled" -> true
  )

  private lazy val repo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  private lazy val individualRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  private val td = TdRiskingInstancesInStates.approved
  private val record: ApplicationForRisking = td.application

  private def rawDocumentFor(record: ApplicationForRisking): Document =
    mongoComponent.database
      .getCollection("application-for-risking")
      .find(Filters.eq("applicationReference", record.applicationReference.value))
      .first()
      .toFuture()
      .futureValue

  override def beforeEach(): Unit =
    super.beforeEach()
    repo.collection.drop().toFuture().futureValue
    individualRepo.collection.drop().toFuture().futureValue
    ()

  // This spec proves the repo wiring runs the ApplicationDataEncryption transform — including nested/optional paths — on every write.
  "with FLE enabled the applicationData PII is encrypted at rest" in:
    repo.upsert(record).futureValue

    val rawJson: String = rawDocumentFor(record).toJson()
    val data = record.applicationData
    val contact = data.applicantContactDetails
    val agentDetails = data.agentDetails
    val correspondence = agentDetails.agentCorrespondenceAddress

    rawJson should not include data.internalUserId.value withClue "internalUserId encrypted"
    rawJson should not include data.groupId.value withClue "groupId encrypted"
    rawJson should not include data.applicantCredentials.providerId withClue "providerId encrypted"
    rawJson should not include contact.applicantName.value withClue "applicantName encrypted"
    rawJson should not include contact.telephoneNumber.value withClue "applicant telephoneNumber encrypted"
    rawJson should not include contact.applicantEmailAddress.value withClue "applicant email encrypted"
    rawJson should not include data.amlsDetails.amlsRegistrationNumber.value withClue "amlsRegistrationNumber encrypted"
    rawJson should not include data.amlsDetails.amlsEvidence.value.fileName withClue "amls evidence fileName encrypted"
    rawJson should not include agentDetails.businessName.agentBusinessName withClue "agentBusinessName encrypted"
    rawJson should not include correspondence.addressLine1 withClue "agent correspondence addressLine1 encrypted"
    rawJson should not include correspondence.addressLine2.value withClue "agent correspondence addressLine2 encrypted"
    rawJson should not include correspondence.postalCode.value withClue "agent correspondence postalCode encrypted"

    rawJson should include(record.applicationReference.value) withClue "applicationReference stays plaintext (search key)"

  "with FLE enabled findById round-trips to plaintext" in:
    repo.upsert(record).futureValue
    repo.findById(record.applicationReference).futureValue.value shouldBe record

  "with FLE enabled the aggregation path returns decrypted application+individuals" in:
    repo.upsert(record).futureValue
    individualRepo.upsert(td.individual1).futureValue
    individualRepo.upsert(td.individual2).futureValue

    repo.findApplicationsAwaitingOverallOutcome().futureValue should contain only td.applicationWithIndividuals
