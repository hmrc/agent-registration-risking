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

package uk.gov.hmrc.agentregistrationrisking.crypto

import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.IndividualData
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates

class IndividualDataEncryptionSpec
extends ISpec:

  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "field-level-encryption.enabled" -> true
  )

  private val fieldLevelEncryption: FieldLevelEncryption = app.injector.instanceOf[FieldLevelEncryption]
  private val individualDataEncryption: IndividualDataEncryption = app.injector.instanceOf[IndividualDataEncryption]

  private def encrypt(plain: String): String = fieldLevelEncryption.encrypt(plain)

  private val individualData: IndividualData = TdRiskingInstancesInStates.approved.individual1.individualData
  private val encryptedIndividualData: IndividualData = individualDataEncryption.encrypt(individualData)

  "IndividualDataEncryption.encrypt sets ciphertext on every PII field" - {

    "individualName is encrypted" in:
      encryptedIndividualData.individualName.value shouldBe encrypt(individualData.individualName.value)

    "telephoneNumber is encrypted" in:
      encryptedIndividualData.telephoneNumber.value shouldBe encrypt(individualData.telephoneNumber.value)

    "emailAddress is encrypted" in:
      encryptedIndividualData.emailAddress.value shouldBe encrypt(individualData.emailAddress.value)

    "vrns are encrypted element-wise" in:
      encryptedIndividualData.vrns.map(_.value) shouldBe individualData.vrns.map(v => encrypt(v.value))

    "payeRefs are encrypted element-wise" in:
      encryptedIndividualData.payeRefs.map(_.value) shouldBe individualData.payeRefs.map(p => encrypt(p.value))
  }

  "IndividualDataEncryption.encrypt leaves non-PII / search-key fields untouched" - {

    "personReference stays plaintext (search key)" in:
      encryptedIndividualData.personReference shouldBe individualData.personReference

    "isPersonOfControl stays plaintext" in:
      encryptedIndividualData.isPersonOfControl shouldBe individualData.isPersonOfControl

    "individualDateOfBirth stays plaintext (not a String-backed field)" in:
      encryptedIndividualData.individualDateOfBirth shouldBe individualData.individualDateOfBirth

    "passedIv stays plaintext" in:
      encryptedIndividualData.passedIv shouldBe individualData.passedIv
  }

  "IndividualDataEncryption handles every IndividualNino / IndividualSaUtr branch" - {

    "IndividualNino.NotProvided is unchanged" in:
      individualDataEncryption.encrypt(individualData.copy(individualNino = IndividualNino.NotProvided)).individualNino shouldBe IndividualNino.NotProvided

    "IndividualSaUtr.NotProvided is unchanged" in:
      individualDataEncryption.encrypt(individualData.copy(individualSaUtr = IndividualSaUtr.NotProvided)).individualSaUtr shouldBe IndividualSaUtr.NotProvided

  }

  "IndividualDataEncryption round-trips and does not leak plaintext PII" - {

    "decrypt(encrypt(x)) == x" in:
      individualDataEncryption.decrypt(individualDataEncryption.encrypt(individualData)) shouldBe individualData

    "rendered JSON of the encrypted individualData contains no plaintext PII" in:
      val rendered: String = Json.toJson(encryptedIndividualData).toString
      val plaintextPii: List[String] =
        List(
          individualData.individualName.value,
          individualData.telephoneNumber.value,
          individualData.emailAddress.value
        ) ++
          (individualData.individualNino match { case IndividualNino.Provided(n) => List(n.value); case _ => Nil }) ++
          (individualData.individualSaUtr match { case IndividualSaUtr.Provided(s) => List(s.value); case _ => Nil }) ++
          individualData.vrns.map(_.value) ++
          individualData.payeRefs.map(_.value)

      plaintextPii.foreach: plaintext =>
        withClue(s"plaintext '$plaintext' must not appear as a JSON value in the encrypted JSON: "):
          rendered should not include s"\"$plaintext\""
  }
