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

package uk.gov.hmrc.agentregistrationrisking.services

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.CheckResult
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.connectors.SdesProxyConnector
import uk.gov.hmrc.agentregistrationrisking.model.CorrelationIdGenerator
import uk.gov.hmrc.agentregistrationrisking.model.sdes.NotifySdesAudit
import uk.gov.hmrc.agentregistrationrisking.model.sdes.NotifySdesFile
import uk.gov.hmrc.agentregistrationrisking.model.sdes.NotifySdesFileReadyChecksum
import uk.gov.hmrc.agentregistrationrisking.model.sdes.NotifySdesFileReadyRequest
import uk.gov.hmrc.agentregistrationrisking.model.sdes.SdesChecksumAlgorithm
import uk.gov.hmrc.agentregistrationrisking.model.sdes.SdesInformationType
import uk.gov.hmrc.agentregistrationrisking.model.sdes.SdesSrn
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging
import uk.gov.hmrc.objectstore.client.ObjectSummaryWithMd5

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class SdesProxyService @Inject() (
  sdesProxyConnector: SdesProxyConnector,
  appConfig: AppConfig,
  correlationIdGenerator: CorrelationIdGenerator
)(using ExecutionContext)
extends RequestAwareLogging:

  def notifySdesFileReady(objectSummaryWithMd5: ObjectSummaryWithMd5)(using RequestHeader): Future[Unit] =
    val notifySdesFileReadyRequest: NotifySdesFileReadyRequest = makeNotifySdesFileReadyRequest(objectSummaryWithMd5)
    sdesProxyConnector.notifySdesFileReady(notifySdesFileReadyRequest)

  private def makeNotifySdesFileReadyRequest(objectSummaryWithMd5: ObjectSummaryWithMd5): NotifySdesFileReadyRequest =
    val informationType: SdesInformationType = appConfig.SdesProxy.outboundInformationType
    val serviceReferenceNumber: SdesSrn = appConfig.SdesProxy.srn
    NotifySdesFileReadyRequest(
      informationType = informationType,
      file = NotifySdesFile(
        recipientOrSender = Some(serviceReferenceNumber),
        name = objectSummaryWithMd5.location.fileName,
        location = Some(objectSummaryWithMd5.location.asUri),
        checksum = NotifySdesFileReadyChecksum(
          algorithm = SdesChecksumAlgorithm.md5,
          value = objectSummaryWithMd5.contentMd5.value
        ),
        size = objectSummaryWithMd5.contentLength.intValue,
        properties = None
      ),
      audit = NotifySdesAudit(correlationIdGenerator.nextCorrelationId)
    )
