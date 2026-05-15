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
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.IndividualData

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IndividualDataEncryption @Inject() (fieldLevelEncryption: FieldLevelEncryption):

  def encrypt(data: IndividualData): IndividualData = transform(data, fieldLevelEncryption.encrypt)
  def decrypt(data: IndividualData): IndividualData = transform(data, fieldLevelEncryption.decrypt)

  private def transform(
    data: IndividualData,
    cryptoOp: String => String
  ): IndividualData = data
    .modify(_.individualName.value).using(cryptoOp)
    .modify(_.internalUserId.value).using(cryptoOp)
    .modify(_.telephoneNumber.value).using(cryptoOp)
    .modify(_.emailAddress.value).using(cryptoOp)
    .modify(_.individualNino).using(transformIndividualNino(_, cryptoOp))
    .modify(_.individualSaUtr).using(transformIndividualSaUtr(_, cryptoOp))
    .modify(_.vrns.each.value).using(cryptoOp)
    .modify(_.payeRefs.each.value).using(cryptoOp)

  private def transformIndividualNino(
    n: IndividualNino,
    cryptoOp: String => String
  ): IndividualNino =
    n match
      case IndividualNino.NotProvided => n
      case p: IndividualNino.Provided => p.modify(_.nino.value).using(cryptoOp)
      case a: IndividualNino.FromAuth => a.modify(_.nino.value).using(cryptoOp)

  private def transformIndividualSaUtr(
    s: IndividualSaUtr,
    cryptoOp: String => String
  ): IndividualSaUtr =
    s match
      case IndividualSaUtr.NotProvided => s
      case p: IndividualSaUtr.Provided => p.modify(_.saUtr.value).using(cryptoOp)
      case a: IndividualSaUtr.FromAuth => a.modify(_.saUtr.value).using(cryptoOp)
      case c: IndividualSaUtr.FromCitizenDetails => c.modify(_.saUtr.value).using(cryptoOp)
