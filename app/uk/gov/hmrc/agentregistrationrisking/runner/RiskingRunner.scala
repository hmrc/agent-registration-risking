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

package uk.gov.hmrc.agentregistrationrisking.runner

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistrationrisking.services.Crypto
import uk.gov.hmrc.agentregistrationrisking.services.ObjectStoreService
import uk.gov.hmrc.agentregistrationrisking.services.RiskingFileService
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging
import uk.gov.hmrc.objectstore.client.ObjectSummaryWithMd5
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class RiskingRunner @Inject() (
  objectStoreService: ObjectStoreService,
  riskingFileService: RiskingFileService,
  crypto: Crypto
)(using ec: ExecutionContext)
extends RequestAwareLogging:

  def run()(using request: RequestHeader): Future[Unit] =
    logger.info("Running risking started ...")

    for
      fileContent <- riskingFileService.buildRiskingFile
      encryptedFileContent = crypto.encrypt(fileContent)
      objectSummary: ObjectSummaryWithMd5 <- objectStoreService.put(encryptedFileContent)
      _ = logger.info(s"File uploaded to object store: ${objectSummary.location}")
    yield ()
