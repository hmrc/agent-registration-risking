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
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.connectors.RiskingResultsFileConnector
import uk.gov.hmrc.agentregistrationrisking.connectors.SdesProxyConnector
import uk.gov.hmrc.agentregistrationrisking.model.CorrelationIdGenerator
import uk.gov.hmrc.agentregistrationrisking.model.sdes.*
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationrisking.util.Utils.*
import uk.gov.hmrc.objectstore.client.ObjectSummaryWithMd5

import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class SdesProxyService @Inject() (
  sdesProxyConnector: SdesProxyConnector,
  appConfig: AppConfig,
  correlationIdGenerator: CorrelationIdGenerator
)(using
  ExecutionContext,
  Clock
)
extends RequestAwareLogging:

  def notifySdesFileReady(objectSummaryWithMd5: ObjectSummaryWithMd5)(using RequestHeader): Future[Unit] =
    val fileReadyNotification = makeNotifySdesFileReadyRequest(objectSummaryWithMd5)
    sdesProxyConnector.notifySdesFileReady(fileReadyNotification)

  private def makeNotifySdesFileReadyRequest(objectSummaryWithMd5: ObjectSummaryWithMd5)(using RequestHeader): NotifySdesFileReadyRequest =
    val informationType: SdesInformationType = appConfig.SdesProxy.outboundInformationType
    val serviceReferenceNumber: SdesSrn = appConfig.SdesProxy.srn
    val objectStoreLocation = s"${appConfig.SdesProxy.objectStoreLocationPrefix}/${objectSummaryWithMd5.location.directory.asUri}"
    val fileName = s"${objectSummaryWithMd5.location.fileName}"

    NotifySdesFileReadyRequest(
      informationType = informationType,
      file = NotifySdesFile(
        recipientOrSender = Some(serviceReferenceNumber),
        name = objectSummaryWithMd5.location.fileName,
        location = Some(s"$objectStoreLocation$fileName"),
        checksum = NotifySdesFileReadyChecksum(
          algorithm = SdesChecksumAlgorithm.md5,
          value = base64ToHex(objectSummaryWithMd5.contentMd5.value)
        ),
        size = objectSummaryWithMd5.contentLength.intValue,
        properties = None
      ),
      audit = NotifySdesAudit(correlationIdGenerator.nextCorrelationId)
    )
