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

import com.softwaremill.quicklens.*
import play.api.libs.json.OFormat
import uk.gov.hmrc.agentregistrationrisking.model.CompletedRisking

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompletedRiskingEncryption @Inject() (
  applicationDataEncryption: ApplicationDataEncryption,
  individualDataEncryption: IndividualDataEncryption
):

  val formats: OFormat[CompletedRisking] = OFormat[CompletedRisking](
    r = CompletedRisking.format.map[CompletedRisking](decrypt),
    w = CompletedRisking.format.contramap[CompletedRisking](encrypt)
  )

  def encrypt(cr: CompletedRisking): CompletedRisking = cr
    .modify(_.application).using(applicationDataEncryption.encrypt)
    .modify(_.individuals.each).using(individualDataEncryption.encrypt)

  def decrypt(cr: CompletedRisking): CompletedRisking = cr
    .modify(_.application).using(applicationDataEncryption.decrypt)
    .modify(_.individuals.each).using(individualDataEncryption.decrypt)
