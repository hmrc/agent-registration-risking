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
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicationData

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationDataEncryption @Inject() (fieldLevelEncryption: FieldLevelEncryption):

  def encrypt(data: ApplicationData): ApplicationData = transform(data, fieldLevelEncryption.encrypt)
  def decrypt(data: ApplicationData): ApplicationData = transform(data, fieldLevelEncryption.decrypt)

  private def transform(
    data: ApplicationData,
    cryptoOp: String => String
  ): ApplicationData = data
    .modify(_.internalUserId.value).using(cryptoOp)
    .modify(_.applicantCredentials.providerId).using(cryptoOp)
    .modify(_.groupId.value).using(cryptoOp)
    .modify(_.applicantContactDetails.applicantName.value).using(cryptoOp)
    .modify(_.applicantContactDetails.telephoneNumber.value).using(cryptoOp)
    .modify(_.applicantContactDetails.applicantEmailAddress.value).using(cryptoOp)
    .modify(_.amlsDetails.amlsRegistrationNumber.value).using(cryptoOp)
    .modify(_.amlsDetails.amlsEvidence.each.fileName).using(cryptoOp)
    .modify(_.agentDetails.businessName.agentBusinessName).using(cryptoOp)
    .modify(_.agentDetails.businessName.otherAgentBusinessName.each).using(cryptoOp)
    .modify(_.agentDetails.telephoneNumber.agentTelephoneNumber).using(cryptoOp)
    .modify(_.agentDetails.telephoneNumber.otherAgentTelephoneNumber.each).using(cryptoOp)
    .modify(_.agentDetails.agentEmailAddress.value).using(cryptoOp)
    .modify(_.agentDetails.agentCorrespondenceAddress.addressLine1).using(cryptoOp)
    .modify(_.agentDetails.agentCorrespondenceAddress.addressLine2.each).using(cryptoOp)
    .modify(_.agentDetails.agentCorrespondenceAddress.addressLine3.each).using(cryptoOp)
    .modify(_.agentDetails.agentCorrespondenceAddress.addressLine4.each).using(cryptoOp)
    .modify(_.agentDetails.agentCorrespondenceAddress.postalCode.each).using(cryptoOp)
    .modify(_.vrns.each.value).using(cryptoOp)
    .modify(_.payeRefs.each.value).using(cryptoOp)
    .modify(_.crn.each.value).using(cryptoOp)
    .modify(_.utr.value).using(cryptoOp)
    .modify(_.safeId.value).using(cryptoOp)
