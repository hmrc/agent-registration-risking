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
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFile
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileName
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec

/** RiskingFileRepo holds no PII so its Mongo `domainFormat` is the plain `RiskingFile.format` (no encryption wrapper). These tests pin the round-trip via that
  * unwrapped path.
  */
class RiskingFileRepoSpec
extends ISpec:

  private lazy val repo: RiskingFileRepo = app.injector.instanceOf[RiskingFileRepo]

  private val riskingFile: RiskingFile = RiskingFile(
    riskingFileName = RiskingFileName("asa_risking_file_version1_0_4_20591125_163351.txt"),
    uploadedAt = frozenInstant
  )

  override def beforeEach(): Unit =
    super.beforeEach()
    repo.collection.drop().toFuture().futureValue
    ()

  "upsert then findById round-trips the record unchanged" in:
    repo.upsert(riskingFile).futureValue
    repo.findById(riskingFile.riskingFileName).futureValue.value shouldBe riskingFile

  "findById returns None when nothing is stored" in:
    repo.findById(riskingFile.riskingFileName).futureValue shouldBe None

  "upsert replaces an existing record" in:
    repo.upsert(riskingFile).futureValue
    val updated = riskingFile.copy(uploadedAt = frozenInstant.plusSeconds(60))
    repo.upsert(updated).futureValue
    repo.findById(riskingFile.riskingFileName).futureValue.value shouldBe updated

  "removeById deletes the record" in:
    repo.upsert(riskingFile).futureValue
    repo.removeById(riskingFile.riskingFileName).futureValue
    repo.findById(riskingFile.riskingFileName).futureValue shouldBe None
