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

  "with FLE disabled the agentApplication PII is stored as plaintext" in:
    repo.upsert(record).futureValue

    val rawJson: String = rawDocumentFor(record).toJson()
    val agentApp = record.agentApplication
    val contact = agentApp.getApplicantContactDetails
    val agentDetails = agentApp.getAgentDetails
    val correspondence = agentDetails.agentCorrespondenceAddress.value

    rawJson should include(agentApp.internalUserId.value) withClue "internalUserId plaintext"
    rawJson should include(agentApp.groupId.value) withClue "groupId plaintext"
    rawJson should include(agentApp.applicantCredentials.providerId) withClue "providerId plaintext"
    rawJson should include(contact.applicantName.value) withClue "applicantName plaintext"
    rawJson should include(contact.telephoneNumber.value.value) withClue "applicant telephoneNumber plaintext"
    rawJson should include(contact.applicantEmailAddress.value.emailAddress.value) withClue "applicant email plaintext"
    rawJson should include(agentDetails.businessName.agentBusinessName) withClue "agentBusinessName plaintext"
    rawJson should include(correspondence.addressLine1) withClue "agent correspondence addressLine1 plaintext"

  "with FLE disabled findById returns the model unchanged" in:
    repo.upsert(record).futureValue
    repo.findById(record.applicationReference).futureValue.value shouldBe record

  "with FLE disabled the aggregation path returns the application+individuals unchanged" in:
    repo.upsert(record).futureValue
    individualRepo.upsert(td.individual1).futureValue
    individualRepo.upsert(td.individual2).futureValue

    repo.findApplicationsAwaitingOverallOutcome().futureValue should contain only td.applicationWithIndividuals
