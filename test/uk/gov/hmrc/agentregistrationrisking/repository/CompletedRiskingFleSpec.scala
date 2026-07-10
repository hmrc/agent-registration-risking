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
import uk.gov.hmrc.agentregistrationrisking.model.CompletedRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates

class CompletedRiskingFleSpec
extends ISpec:

  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "field-level-encryption.enabled" -> true
  )

  private lazy val completedRiskingRepo: CompletedRiskingRepo = app.injector.instanceOf[CompletedRiskingRepo]

  private val completedRisking: CompletedRisking = TdRiskingInstancesInStates.failedFixableAfterBackendNotified.completedRisking

  private def rawDocumentFor(completedRisking: CompletedRisking): Document =
    mongoComponent.database
      .getCollection(completedRiskingRepo.collectionName)
      .find(Filters.eq(FieldNames.CompletedRisking.applicationReference, completedRisking.application.applicationReference.value))
      .first()
      .toFuture()
      .futureValue

  override def beforeEach(): Unit =
    super.beforeEach()
    completedRiskingRepo.collection.drop().toFuture().futureValue
    ()

  "with FLE enabled the nested application.applicationData PII is encrypted at rest" in:
    completedRiskingRepo.upsert(completedRisking).futureValue

    val rawJson: String = rawDocumentFor(completedRisking).toJson()
    val applicationData = completedRisking.application.applicationData
    val applicantContactDetails = applicationData.applicantContactDetails
    val agentDetails = applicationData.agentDetails
    val agentCorrespondenceAddress = agentDetails.agentCorrespondenceAddress

    rawJson should not include applicationData.internalUserId.value withClue "internalUserId encrypted"
    rawJson should not include applicationData.groupId.value withClue "groupId encrypted"
    rawJson should not include applicantContactDetails.applicantName.value withClue "applicantName encrypted"
    rawJson should not include applicantContactDetails.applicantEmailAddress.value withClue "applicant email encrypted"
    rawJson should not include applicationData.utr.value withClue "utr encrypted"
    rawJson should not include applicationData.safeId.value withClue "safeId encrypted"
    rawJson should not include agentDetails.agentEmailAddress.value withClue "agent email encrypted"
    rawJson should not include agentCorrespondenceAddress.addressLine1 withClue "addressLine1 encrypted"

    rawJson should include(completedRisking.application.applicationReference.value) withClue "applicationReference stays plaintext (findRecent search key)"

  "with FLE enabled every nested individual's individualData PII is encrypted at rest" in:
    completedRiskingRepo.upsert(completedRisking).futureValue

    val rawJson: String = rawDocumentFor(completedRisking).toJson()

    completedRisking.individuals.foreach: individual =>
      val d = individual.individualData
      rawJson should not include d.individualName.value withClue s"individualName encrypted for ${individual.personReference.value}"
      rawJson should not include d.telephoneNumber.value withClue s"individual telephoneNumber encrypted for ${individual.personReference.value}"
      rawJson should not include d.emailAddress.value withClue s"individual email encrypted for ${individual.personReference.value}"
      rawJson should include(
        individual.personReference.value
      ) withClue s"personReference stays plaintext (findRecent search key) for ${individual.personReference.value}"

  "with FLE enabled findById round-trips to plaintext" in:
    completedRiskingRepo.upsert(completedRisking).futureValue
    completedRiskingRepo.findById(completedRisking._id).futureValue.value shouldBe completedRisking

  "with FLE enabled findRecent(applicationReference) round-trips to plaintext" in:
    completedRiskingRepo.upsert(completedRisking).futureValue
    completedRiskingRepo.findRecent(completedRisking.application.applicationReference).futureValue.value shouldBe completedRisking

  "with FLE enabled findRecent(personReference) round-trips to plaintext" in:
    completedRiskingRepo.upsert(completedRisking).futureValue
    completedRiskingRepo.findRecent(completedRisking.individuals.head.personReference).futureValue.value shouldBe completedRisking
