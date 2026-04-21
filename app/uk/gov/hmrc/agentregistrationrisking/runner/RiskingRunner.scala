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

import play.api.mvc.Headers
import play.api.mvc.RequestHeader
import play.api.mvc.request.RemoteConnection
import play.api.mvc.request.RequestTarget
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileId
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.services.ObjectStoreService
import uk.gov.hmrc.agentregistrationrisking.services.RiskingFileService
import uk.gov.hmrc.agentregistrationrisking.services.SdesProxyService
import uk.gov.hmrc.objectstore.client.ObjectSummaryWithMd5
import play.api.libs.typedmap.TypedMap
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class RiskingRunner @Inject() (
  objectStoreService: ObjectStoreService,
  riskingFileService: RiskingFileService,
  sdesProxyService: SdesProxyService,
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  individualForRiskingRepo: IndividualForRiskingRepo
)(using ec: ExecutionContext)
extends RequestAwareLogging:

  private val emptyRequestHeader: RequestHeader =
    new RequestHeader:
      def target: RequestTarget = RequestTarget(
        uriString = "/",
        path = "riskingRunner/dummyPath",
        queryString = Map.empty
      )
      def version: String = "HTTP/1.1"
      def method: String = "GET"
      def headers: Headers = Headers()
      def connection: RemoteConnection = RemoteConnection(
        remoteAddress = java.net.InetAddress.getLoopbackAddress,
        secure = false,
        clientCertificateChain = None
      )
      def attrs: TypedMap = TypedMap.empty

  // outbound flow
  def run(): Future[Unit] =
    given RequestHeader = emptyRequestHeader
    logger.info("Running risking started ...")

    for
      applicationsWithIndividuals <- riskingFileService.getApplicationsReadyForRiskingWithIndividuals
      fileContent: String = riskingFileService.buildRiskingFileFrom(applicationsWithIndividuals)
      objectSummary: ObjectSummaryWithMd5 <- objectStoreService.put(fileContent)
      _ <- sdesProxyService.notifySdesFileReady(objectSummary)
      riskingFileId = RiskingFileId(objectSummary.location.fileName)
      _ <- markAsSubmittedForRisking(applicationsWithIndividuals, riskingFileId)
      _ = logger.info(s"File uploaded to object store: ${objectSummary.location}")
    yield ()

  private def markAsSubmittedForRisking(
    applicationsWithIndividuals: Seq[ApplicationWithIndividuals],
    riskingFileId: RiskingFileId
  ): Future[Unit] = Future.traverse(applicationsWithIndividuals): appWithIndividuals =>
    for
      _ <- applicationForRiskingRepo.upsert(appWithIndividuals.application.copy(riskingFileId = Some(riskingFileId)))
      _ <-
        Future.traverse(appWithIndividuals.individuals): individual =>
          individualForRiskingRepo.upsert(individual.copy(riskingFileId = Some(riskingFileId)))
    yield ()
  .map(_ => ())
