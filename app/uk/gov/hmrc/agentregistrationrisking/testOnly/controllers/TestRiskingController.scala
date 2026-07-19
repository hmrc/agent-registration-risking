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

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistrationrisking.action.Actions
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.model.*
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.FieldNames.applicationReference
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.CompletedRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.RiskingFileRepo
import uk.gov.hmrc.agentregistrationrisking.runner.RiskingFileUploadRunner
import uk.gov.hmrc.agentregistrationrisking.services.RiskingFileService
import uk.gov.hmrc.agentregistrationrisking.services.SdesProxyService
import uk.gov.hmrc.agentregistrationrisking.testOnly.util.TestMongoCleanup
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import scala.annotation.nowarn
import scala.concurrent.ExecutionContext
import scala.util.Random
import scala.concurrent.Future

@Singleton()
@nowarn()
class TestRiskingController @Inject() (
  cc: ControllerComponents,
  actions: Actions,
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  individualForRiskingRepo: IndividualForRiskingRepo,
  completedRiskingRepo: CompletedRiskingRepo,
  applicationForRiskingIdGenerator: ApplicationForRiskingIdGenerator,
  agentReferenceGenerator: ApplicationReferenceGenerator,
  personReferenceGenerator: PersonReferenceGenerator,
  riskingFileUploadRunner: RiskingFileUploadRunner,
  sdesProxyService: SdesProxyService,
  riskingFileService: RiskingFileService,
  riskingFileRepo: RiskingFileRepo,
  testMongoCleanup: TestMongoCleanup,
  appConfig: AppConfig
)(using
  clock: Clock
)
extends BackendController(cc)
with Logging:

  given ExecutionContext = controllerComponents.executionContext

  def findApplicationForRisking(applicationReference: ApplicationReference): Action[AnyContent] = Action
    .async:
      implicit request =>
        for
          maybeApplication <- applicationForRiskingRepo.findById(applicationReference)
        yield maybeApplication match
          case None => NoContent
          case Some(applicationForRisking) => Ok(Json.prettyPrint(Json.toJson(applicationForRisking)))

  def findIndividualsForRisking(applicationReference: ApplicationReference): Action[AnyContent] = Action
    .async:
      implicit request =>
        for
          individuals <- individualForRiskingRepo.findByApplicationReference(applicationReference)
        yield Ok(Json.prettyPrint(Json.toJson(individuals)))

  def findCompletedRisking(applicationReference: ApplicationReference): Action[AnyContent] = Action
    .async:
      implicit request =>
        for
          completedRisking <- completedRiskingRepo.findRecent(applicationReference)
        yield Ok(Json.prettyPrint(Json.toJson(completedRisking)))

  def runRisking: Action[AnyContent] = Action
    .async:
      implicit request =>
        for
          _ <- riskingFileUploadRunner.run()
        yield Ok

  def viewNextRiskingFileContents: Action[AnyContent] = Action
    .async:
      implicit request =>
        for
          (riskingFileWithContent, _) <- riskingFileUploadRunner.buildRiskingFile()
          s: String = riskingFileWithContent.riskingFileContent
        yield Ok(s)

  def deleteAllApplications: Action[AnyContent] = Action
    .async:
      implicit request =>
        for
          _ <- testMongoCleanup.deleteAllApplications
          _ <- testMongoCleanup.deleteAllIndividuals
          _ <- testMongoCleanup.deleteAllRiskingFiles
        yield NoContent

  private def generateRandomName(): String =
    val firstNames = List(
      "John",
      "Jane",
      "Michael",
      "Sarah",
      "David",
      "Emma",
      "James",
      "Emily",
      "Robert",
      "Olivia"
    )
    val lastNames = List(
      "Smith",
      "Johnson",
      "Williams",
      "Brown",
      "Jones",
      "Garcia",
      "Miller",
      "Davis",
      "Rodriguez",
      "Martinez"
    )
    val random = new Random()
    s"${firstNames.lift(random.nextInt(firstNames.length)).getOrElse("")} ${lastNames.lift(random.nextInt(lastNames.length)).getOrElse("")}"

  private def generateRandomDateOfBirth(): LocalDate =
    val random = new Random()
    val minYear = 1940
    val maxYear = 2005
    val year = minYear + random.nextInt(maxYear - minYear + 1)
    val month = 1 + random.nextInt(12)
    val maxDay = LocalDate.of(year, month, 1).lengthOfMonth()
    val day = 1 + random.nextInt(maxDay)
    LocalDate.of(
      year,
      month,
      day
    )

  private def generateRandomVrn(): String =
    val random = new Random()
    val part1 = 100 + random.nextInt(900) // 3 digits: 100-999
    val part2 = 1000 + random.nextInt(9000) // 4 digits: 1000-9999
    val part3 = 10 + random.nextInt(90) // 2 digits: 10-99
    s"GB$part1 $part2 $part3"

  private def generateRandomPayeRef(): String =
    val random = new Random()
    val alphanumeric = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    val part1 = (1 to 3).map(_ => alphanumeric(random.nextInt(alphanumeric.length))).mkString
    val part2 = (1 to 5).map(_ => alphanumeric(random.nextInt(alphanumeric.length))).mkString
    s"$part1/$part2"

  private def generateRandomNino(): Nino =
    val random = new Random()
    val validPrefixes = "ABCEGHJKLMNOPRSTWXYZ"
    val validSuffixes = "ABCD"
    val prefix1 = validPrefixes(random.nextInt(validPrefixes.length))
    val prefix2 = validPrefixes(random.nextInt(validPrefixes.length))
    val digits = (1 to 6).map(_ => random.nextInt(10)).mkString
    val suffix = validSuffixes(random.nextInt(validSuffixes.length))
    Nino(s"$prefix1$prefix2$digits$suffix")

  private def generateRandomSaUtr(): SaUtr =
    val random = new Random()
    val digits = (1 to 10).map(_ => random.nextInt(10)).mkString
    SaUtr(digits)
