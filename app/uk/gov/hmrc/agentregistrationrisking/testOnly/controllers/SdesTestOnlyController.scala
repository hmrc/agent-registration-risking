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

package uk.gov.hmrc.agentregistrationrisking.testOnly.controllers

import org.mongodb.scala.SingleObservableFuture
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.Configuration
import play.api.Logging
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistrationrisking.action.Actions
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.model.*
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.runner.RiskingFileUploadRunner
import uk.gov.hmrc.agentregistrationrisking.runner.RiskingResultsFileProcessingRunner
import uk.gov.hmrc.agentregistrationrisking.services.SdesProxyService
import uk.gov.hmrc.agentregistrationrisking.testOnly.model.RiskingResultsFileContent
import uk.gov.hmrc.agentregistrationrisking.testOnly.model.RiskingResultsFileName
import uk.gov.hmrc.agentregistrationrisking.testOnly.repos.RiskingResultsFileContentsRepo
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import scala.annotation.nowarn
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton()
@nowarn()
class SdesTestOnlyController @Inject() (
  cc: ControllerComponents,
  actions: Actions,
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  individualForRiskingRepo: IndividualForRiskingRepo,
  applicationForRiskingIdGenerator: ApplicationForRiskingIdGenerator,
  agentReferenceGenerator: ApplicationReferenceGenerator,
  personReferenceGenerator: PersonReferenceGenerator,
  riskingFileUploadRunner: RiskingFileUploadRunner,
  sdesProxyService: SdesProxyService,
  appConfig: AppConfig,
  configuration: Configuration,
  riskingResultsFileContentsRepo: RiskingResultsFileContentsRepo,
  riskingResultsFileProcessingRunner: RiskingResultsFileProcessingRunner
)(using clock: Clock)
extends BackendController(cc)
with Logging:

  given ExecutionContext = controllerComponents.executionContext

  val thisBackendBaseUrl: String =
    val port: Int =
      val maybePort: Option[Int] = configuration.getOptional[Int]("http.port") // this is set up only on higher level environments
      maybePort.getOrElse(22203) // default to the port from build.sbt
    s"http://localhost:$port"

  def listAvailableFiles(informationType: String): Action[AnyContent] = actions
    .default
    .async:
      implicit request =>
        riskingResultsFileContentsRepo
          .findAll()
          .map: (riskingResultsFileContents: Seq[RiskingResultsFileContent]) =>
            logger.info(s"Found ${riskingResultsFileContents.size} risking results files")
            riskingResultsFileContents
              .map(_.riskingResultsFileName)
              .map(makeAvailableFileJson)
          .map(x => Json.prettyPrint(Json.toJson(x)))
          .map(Ok(_))

  private def downloadUrl(riskingResultsFileName: RiskingResultsFileName): String =
    thisBackendBaseUrl + routes.SdesTestOnlyController.downloadRiskingResultsFile(riskingResultsFileName)

  private def makeAvailableFileJson(riskingResultsFileName: RiskingResultsFileName) = Json.obj(
    "id" -> riskingResultsFileName,
    "filename" -> riskingResultsFileName,
    "downloadURL" -> downloadUrl(riskingResultsFileName),
    "fileSize" -> 1234,
    "metadata" -> Json.arr(
      Json.obj("metadata" -> "periodStartYear", "value" -> 2018),
      Json.obj("metadata" -> "periodStartMonth", "value" -> 6),
      Json.obj("metadata" -> "fileType", "value" -> "CSV"),
      Json.obj("metadata" -> "fileRole", "value" -> "pvat")
    )
  )

  def uploadRiskingResultsFile(
    riskingResultsFileName: RiskingResultsFileName
  ): Action[JsValue] =
    actions
      .default
      .async(parse.json):
        implicit request =>
          riskingResultsFileContentsRepo.findById(riskingResultsFileName).flatMap:
            case Some(_) => Future.successful(Conflict(s"Risking results file already exists: $riskingResultsFileName"))
            case None =>
              riskingResultsFileContentsRepo.upsert(
                RiskingResultsFileContent(
                  riskingResultsFileName = riskingResultsFileName,
                  content = request.body,
                  uploadedAt = Instant.now(clock)
                )
              ).map(_ => Ok(""))

  def downloadRiskingResultsFile(
    riskingResultsFileName: RiskingResultsFileName
  ): Action[AnyContent] = actions
    .default
    .async:
      implicit request =>
        riskingResultsFileContentsRepo
          .findById(riskingResultsFileName)
          .map:
            case Some(riskingResultsFileContent) => Ok(Json.prettyPrint(Json.toJson(riskingResultsFileContent.content)))
            case None => NotFound(s"Risking results file not found: $riskingResultsFileName")

  def runResultsFileProcessingJob(): Action[AnyContent] = actions
    .default
    .async:
      implicit request =>
        riskingResultsFileProcessingRunner.run().map(_ => Ok(""))

  def deleteRiskingResultsFile(
    riskingResultsFileName: RiskingResultsFileName
  ): Action[AnyContent] = actions
    .default
    .async:
      implicit request =>
        riskingResultsFileContentsRepo
          .removeById(riskingResultsFileName)
          .map(_ => Ok(""))

  def deleteAllRiskingResultsFiles(): Action[AnyContent] = actions
    .default
    .async:
      implicit request =>
        riskingResultsFileContentsRepo
          .collection
          .drop()
          .toFuture
          .map(_ => Ok(""))
