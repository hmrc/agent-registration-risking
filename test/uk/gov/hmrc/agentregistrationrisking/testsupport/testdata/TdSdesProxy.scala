/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationrisking.testsupport.testdata

import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.individual.IndividualDateOfBirth.Provided
import uk.gov.hmrc.agentregistration.shared.individual.IndividualDateOfBirth
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.risking.IndividualRiskingResponse
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase
import uk.gov.hmrc.agentregistrationrisking.model.sdes.AvailableFile
import uk.gov.hmrc.agentregistrationrisking.model.sdes.NotifySdesAudit
import uk.gov.hmrc.agentregistrationrisking.model.sdes.NotifySdesFile
import uk.gov.hmrc.agentregistrationrisking.model.sdes.NotifySdesFileReadyChecksum
import uk.gov.hmrc.agentregistrationrisking.model.sdes.NotifySdesFileReadyRequest
import uk.gov.hmrc.agentregistrationrisking.model.sdes.SdesChecksumAlgorithm
import uk.gov.hmrc.agentregistrationrisking.model.sdes.SdesInformationType
import uk.gov.hmrc.agentregistrationrisking.model.sdes.SdesSrn
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.correlationId
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.objectSummaryWithMd5
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.testAvailableFile

import java.time.Instant

trait TdSdesProxy { dependencies: TdBase =>

  private val createdAt: Instant = dependencies.nowAsInstant

  def sdesFileData(fileName: String): AvailableFile = AvailableFile(
    downloadURL = testAvailableFile.downloadURL,
    filename = fileName,
    fileSize = 1024
  )

  def notifySdesFileReadyRequest: NotifySdesFileReadyRequest = NotifySdesFileReadyRequest(
    informationType = SdesInformationType("test-outbound-information-type"),
    file = NotifySdesFile(
      recipientOrSender = Some(SdesSrn("test-srn")),
      name = objectSummaryWithMd5.location.fileName,
      location = Some("/agent-registration-risking/applications-for-risking/"),
      checksum = NotifySdesFileReadyChecksum(
        algorithm = SdesChecksumAlgorithm.md5,
        value = objectSummaryWithMd5.contentMd5.value
      ),
      size = objectSummaryWithMd5.contentLength.toInt,
      properties = None
    ),
    audit = NotifySdesAudit(correlationID = correlationId)
  )

}
