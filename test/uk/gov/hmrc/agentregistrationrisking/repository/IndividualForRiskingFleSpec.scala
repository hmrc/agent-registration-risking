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
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates

class IndividualForRiskingFleSpec
extends ISpec:

  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "field-level-encryption.enabled" -> true
  )

  private lazy val repo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  private val record: IndividualForRisking = TdRiskingInstancesInStates.approved.individual1

  private def rawDocumentFor(record: IndividualForRisking): Document =
    mongoComponent.database
      .getCollection("individual-for-risking")
      .find(Filters.eq("personReference", record.personReference.value))
      .first()
      .toFuture()
      .futureValue

  override def beforeEach(): Unit =
    super.beforeEach()
    repo.collection.drop().toFuture().futureValue
    ()

  // Exhaustive per-PII-path coverage (including IndividualNino/IndividualSaUtr variants) lives in IndividualProvidedDetailsEncryptionSpec in the
  // agent-registration repo (the transform is shared source, synced from there). This spec proves the repo wiring actually runs that transform —
  // including list and sealed-trait paths — on every write.
  "with FLE enabled the individualProvidedDetails PII is encrypted at rest" in:
    repo.upsert(record).futureValue

    val rawJson: String = rawDocumentFor(record).toJson()
    val details = record.individualProvidedDetails

    rawJson should not include details.individualName.value withClue "individualName encrypted"
    rawJson should not include details.getTelephoneNumber.value withClue "telephoneNumber encrypted"
    rawJson should not include details.getEmailAddress.emailAddress.value withClue "emailAddress encrypted"
    details.getNino match
      case IndividualNino.Provided(nino) => rawJson should not include nino.value withClue "nino encrypted"
      case other => fail(s"fixture expected IndividualNino.Provided, got $other")
    details.getSaUtr match
      case IndividualSaUtr.Provided(saUtr) => rawJson should not include saUtr.value withClue "saUtr encrypted"
      case other => fail(s"fixture expected IndividualSaUtr.Provided, got $other")
    details.vrns.value.foreach(vrn => rawJson should not include vrn.value withClue "vrn encrypted element-wise")
    details.payeRefs.value.foreach(payeRef => rawJson should not include payeRef.value withClue "payeRef encrypted element-wise")

    rawJson should include(record.personReference.value) withClue "personReference stays plaintext (search key)"
    rawJson should include(record.applicationReference.value) withClue "applicationReference stays plaintext (search key)"

  "with FLE enabled findById round-trips to plaintext" in:
    repo.upsert(record).futureValue
    repo.findById(record.personReference).futureValue.value shouldBe record

  "with FLE enabled findByApplicationReference returns decrypted individuals" in:
    val record2 = TdRiskingInstancesInStates.approved.individual2
    repo.upsert(record).futureValue
    repo.upsert(record2).futureValue

    repo.findByApplicationReference(record.applicationReference).futureValue.toSet shouldBe Set(record, record2)

  "with FLE enabled insertMany encrypts at rest and round-trips" in:
    val record2 = TdRiskingInstancesInStates.approved.individual2
    repo.insertMany(List(record, record2)).futureValue

    rawDocumentFor(record).toJson() should not include record.individualProvidedDetails.individualName.value withClue "insertMany encrypts at rest"
    repo.findByApplicationReference(record.applicationReference).futureValue.toSet shouldBe Set(record, record2)
