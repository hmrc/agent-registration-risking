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
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.IndividualData
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates

class IndividualForRiskingFleSpec
extends ISpec:

  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "field-level-encryption.enabled" -> true
  )

  private lazy val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  private val individualForRisking: IndividualForRisking = TdRiskingInstancesInStates.approved.individual1

  private def rawDocumentFor(individualForRisking: IndividualForRisking): Document =
    mongoComponent.database
      .getCollection(individualForRiskingRepo.collectionName)
      .find(Filters.eq("personReference", individualForRisking.personReference.value))
      .first()
      .toFuture()
      .futureValue

  override def beforeEach(): Unit =
    super.beforeEach()
    individualForRiskingRepo.collection.drop().toFuture().futureValue
    ()

  // This spec proves the repo wiring runs the IndividualDataEncryption transform — including list and sealed-trait paths — on every write.
  "with FLE enabled the individualData PII is encrypted at rest" in:
    individualForRiskingRepo.upsert(individualForRisking).futureValue

    val rawJson: String = rawDocumentFor(individualForRisking).toJson()
    val individualData: IndividualData = individualForRisking.individualData

    rawJson should not include individualData.individualName.value withClue "individualName encrypted"
    rawJson should not include individualData.telephoneNumber.value withClue "telephoneNumber encrypted"
    rawJson should not include individualData.emailAddress.value withClue "emailAddress encrypted"
    individualData.individualNino match
      case IndividualNino.Provided(nino) => rawJson should not include nino.value withClue "nino encrypted"
      case other => fail(s"fixture expected IndividualNino.Provided, got $other")
    individualData.individualSaUtr match
      case IndividualSaUtr.Provided(saUtr) => rawJson should not include saUtr.value withClue "saUtr encrypted"
      case other => fail(s"fixture expected IndividualSaUtr.Provided, got $other")
    individualData.vrns.foreach(vrn => rawJson should not include vrn.value withClue "vrn encrypted element-wise")
    individualData.payeRefs.foreach(payeRef => rawJson should not include payeRef.value withClue "payeRef encrypted element-wise")

    rawJson should include(individualForRisking.personReference.value) withClue "personReference stays plaintext (search key)"
    rawJson should include(individualForRisking.applicationReference.value) withClue "applicationReference stays plaintext (search key)"

  "with FLE enabled findById round-trips to plaintext" in:
    individualForRiskingRepo.upsert(individualForRisking).futureValue
    individualForRiskingRepo.findById(individualForRisking.personReference).futureValue.value shouldBe individualForRisking

  "with FLE enabled findByApplicationReference returns decrypted individuals" in:
    val individualForRisking2: IndividualForRisking = TdRiskingInstancesInStates.approved.individual2
    individualForRiskingRepo.upsert(individualForRisking).futureValue
    individualForRiskingRepo.upsert(individualForRisking2).futureValue

    individualForRiskingRepo.findByApplicationReference(individualForRisking.applicationReference).futureValue.toSet shouldBe Set(
      individualForRisking,
      individualForRisking2
    )

  "with FLE enabled insertMany encrypts at rest and round-trips" in:
    val individualForRisking2: IndividualForRisking = TdRiskingInstancesInStates.approved.individual2
    individualForRiskingRepo.insertMany(List(individualForRisking, individualForRisking2)).futureValue

    rawDocumentFor(
      individualForRisking
    ).toJson() should not include individualForRisking.individualData.individualName.value withClue "insertMany encrypts at rest"
    individualForRiskingRepo.findByApplicationReference(individualForRisking.applicationReference).futureValue.toSet shouldBe Set(
      individualForRisking,
      individualForRisking2
    )
