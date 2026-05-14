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

  // Exhaustive per-PII-path coverage of the encryption transform lives in AgentApplicationEncryptionSpec in the agent-registration repo
  // (the transform is shared source, synced from there). This spec proves the repo wiring actually runs that transform — including
  // nested/optional paths — on every write.
  "with FLE enabled the agentApplication PII is encrypted at rest" in:
    repo.upsert(record).futureValue

    val rawJson: String = rawDocumentFor(record).toJson()
    val agentApp = record.agentApplication
    val contact = agentApp.getApplicantContactDetails
    val agentDetails = agentApp.getAgentDetails
    val correspondence = agentDetails.agentCorrespondenceAddress.value

    rawJson should not include agentApp.internalUserId.value withClue "internalUserId encrypted"
    rawJson should not include agentApp.groupId.value withClue "groupId encrypted"
    rawJson should not include agentApp.applicantCredentials.providerId withClue "providerId encrypted"
    rawJson should not include contact.applicantName.value withClue "applicantName encrypted"
    rawJson should not include contact.telephoneNumber.value.value withClue "applicant telephoneNumber encrypted"
    rawJson should not include contact.applicantEmailAddress.value.emailAddress.value withClue "applicant email encrypted"
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
