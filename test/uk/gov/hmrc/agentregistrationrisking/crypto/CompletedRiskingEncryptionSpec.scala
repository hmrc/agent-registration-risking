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
import uk.gov.hmrc.agentregistrationrisking.model.CompletedRisking
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates

class CompletedRiskingEncryptionSpec
extends ISpec:

  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "field-level-encryption.enabled" -> true
  )

  private val applicationDataEncryption: ApplicationDataEncryption = app.injector.instanceOf[ApplicationDataEncryption]
  private val individualDataEncryption: IndividualDataEncryption = app.injector.instanceOf[IndividualDataEncryption]
  private val completedRiskingEncryption: CompletedRiskingEncryption = app.injector.instanceOf[CompletedRiskingEncryption]

  private val completedRisking: CompletedRisking = TdRiskingInstancesInStates.failedFixableAfterBackendNotified.completedRisking
  private val encryptedCompletedRisking: CompletedRisking = completedRiskingEncryption.encrypt(completedRisking)

  "CompletedRiskingEncryption" - {

    "delegates encryption of the application field to ApplicationDataEncryption" in:
      encryptedCompletedRisking.application shouldBe applicationDataEncryption.encrypt(completedRisking.application)

    "delegates encryption of each individual to IndividualDataEncryption" in:
      encryptedCompletedRisking.individuals shouldBe completedRisking.individuals.map(individualDataEncryption.encrypt)

    "leaves non-PII fields untouched (_id, completedAt, riskingFile)" in:
      encryptedCompletedRisking._id shouldBe completedRisking._id
      encryptedCompletedRisking.completedAt shouldBe completedRisking.completedAt
      encryptedCompletedRisking.riskingFile shouldBe completedRisking.riskingFile

    "round-trips — decrypt(encrypt(x)) == x" in:
      completedRiskingEncryption.decrypt(completedRiskingEncryption.encrypt(completedRisking)) shouldBe completedRisking

    "rendered JSON of the encrypted CompletedRisking contains no plaintext PII from the nested application or individuals" in:
      val rendered: String = Json.toJson(encryptedCompletedRisking).toString
      val applicationData = completedRisking.application.applicationData
      val plaintextPii: List[String] =
        List(
          applicationData.internalUserId.value,
          applicationData.applicantContactDetails.applicantName.value,
          applicationData.applicantContactDetails.applicantEmailAddress.value,
          applicationData.agentDetails.agentEmailAddress.value,
          applicationData.utr.value,
          applicationData.safeId.value
        ) ++ completedRisking.individuals.flatMap: individual =>
          val d = individual.individualData
          List(
            d.individualName.value,
            d.telephoneNumber.value,
            d.emailAddress.value
          )

      plaintextPii.foreach: plaintext =>
        withClue(s"plaintext '$plaintext' must not appear as a JSON value in the encrypted JSON: "):
          rendered should not include s"\"$plaintext\""
  }
