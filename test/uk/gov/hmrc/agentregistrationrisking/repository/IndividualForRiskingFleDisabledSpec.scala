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
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.IndividualData
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates

/** Regression for the local-dev path where field-level encryption is turned off.
  *
  * In deployed environments the flag is always on; locally it is off so engineers can read Mongo documents as plaintext. The whole encrypt/decrypt pipeline
  * must therefore be a no-op when the flag is false.
  */
class IndividualForRiskingFleDisabledSpec
extends ISpec:

  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "field-level-encryption.enabled" -> false
  )

  private lazy val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  private val individualForRisking: IndividualForRisking = TdRiskingInstancesInStates.approved.individual1

  private def rawDocumentFor(individualForRisking: IndividualForRisking): Document =
    mongoComponent.database
      .getCollection("individual-for-risking")
      .find(Filters.eq("personReference", individualForRisking.personReference.value))
      .first()
      .toFuture()
      .futureValue

  override def beforeEach(): Unit =
    super.beforeEach()
    individualForRiskingRepo.collection.drop().toFuture().futureValue
    ()

  "with FLE disabled the individualData PII is stored as plaintext" in:
    individualForRiskingRepo.upsert(individualForRisking).futureValue

    val rawJson: String = rawDocumentFor(individualForRisking).toJson()
    val individualData: IndividualData = individualForRisking.individualData

    rawJson should include(individualData.individualName.value) withClue "individualName plaintext"
    rawJson should include(individualData.telephoneNumber.value) withClue "telephoneNumber plaintext"
    rawJson should include(individualData.emailAddress.value) withClue "emailAddress plaintext"
    individualData.vrns.foreach(vrn => rawJson should include(vrn.value) withClue "vrn plaintext")
    individualData.payeRefs.foreach(payeRef => rawJson should include(payeRef.value) withClue "payeRef plaintext")

  "with FLE disabled findById returns the model unchanged" in:
    individualForRiskingRepo.upsert(individualForRisking).futureValue
    individualForRiskingRepo.findById(individualForRisking.personReference).futureValue.value shouldBe individualForRisking

  "with FLE disabled findByApplicationReference returns the individuals unchanged" in:
    val individualForRisking2: IndividualForRisking = TdRiskingInstancesInStates.approved.individual2
    individualForRiskingRepo.upsert(individualForRisking).futureValue
    individualForRiskingRepo.upsert(individualForRisking2).futureValue

    individualForRiskingRepo.findByApplicationReference(individualForRisking.applicationReference).futureValue.toSet shouldBe Set(
      individualForRisking,
      individualForRisking2
    )
