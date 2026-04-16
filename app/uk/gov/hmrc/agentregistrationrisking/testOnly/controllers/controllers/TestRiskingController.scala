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

package uk.gov.hmrc.agentregistrationrisking.testOnly.controllers.controllers

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.agentregistration.shared.AmlsCode
import uk.gov.hmrc.agentregistration.shared.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.Crn
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.GroupId
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistration.shared.SafeId
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentBusinessName
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentCorrespondenceAddress
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentDetails
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentEmailAddress
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentTelephoneNumber
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentVerifiedEmailAddress
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.individual.IndividualDateOfBirth.Provided
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationForRiskingStatus
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationReferenceGenerator
import uk.gov.hmrc.agentregistration.shared.risking.PersonReferenceGenerator
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationrisking.action.Actions
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.hip.Arn
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.runner.RiskingRunner
import uk.gov.hmrc.agentregistrationrisking.services.ApplicationStatusService
import uk.gov.hmrc.agentregistrationrisking.services.ResultsFileService
import uk.gov.hmrc.agentregistrationrisking.services.RiskingFileService
import uk.gov.hmrc.agentregistrationrisking.services.SubscribeAgentService
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.agentregistrationrisking.services.SdesProxyService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Random

@Singleton()
class TestRiskingController @Inject() (
  cc: ControllerComponents,
  actions: Actions,
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  agentReferenceGenerator: ApplicationReferenceGenerator,
  personReferenceGenerator: PersonReferenceGenerator,
  riskingFileService: RiskingFileService,
  riskingRunner: RiskingRunner,
  sdesProxyService: SdesProxyService,
  subscribeAgentService: SubscribeAgentService,
  resultsFileService: ResultsFileService,
  applicationStatusService: ApplicationStatusService
)
extends BackendController(cc)
with Logging:

  given ExecutionContext = controllerComponents.executionContext

  def createAndSendRiskingFile: Action[AnyContent] = Action
    .async:
      implicit request =>
        riskingRunner.run().map(_ => Ok)

  def viewNextRiskingFileContents: Action[AnyContent] = Action
    .async:
      implicit request =>
        riskingFileService.getApplicationsReadyForRisking.map: applications =>
          val riskingFile: String = riskingFileService.buildRiskingFileFrom(applications)
          Ok(riskingFile)

  def createTestApplicationForRisking(numberOfIndividuals: Int): Action[AnyContent] = Action
    .async:
      implicit request =>
        val applicationForRisking: ApplicationForRisking = makeApplicationForRisking(numberOfIndividuals)
        applicationForRiskingRepo
          .upsert(applicationForRisking)
          .map(_ => Ok(Json.obj("applicationReference" -> applicationForRisking.applicationReference.value)))

  def downloadAvailableResultsFiles: Action[AnyContent] = Action
    .async:
      implicit request =>
        // Skips the object-store upload step: the object-store stub rejects localhost download
        // URLs with 400, but for local testing we only care about status updates, not archival.
        for
          unprocessedFiles <- resultsFileService.getUnprocessedAvailableFiles()
          _ <-
            unprocessedFiles.foldLeft(Future.unit): (acc, file) =>
              acc.flatMap: _ =>
                for
                  records <- resultsFileService.downloadAndParseRecords(file)
                  _ <- applicationStatusService.processResults(records)
                  updatedApplications <- applicationStatusService.updateApplicationStatuses(records)
                  approvedApplications = updatedApplications.filter(
                    _.status === ApplicationForRiskingStatus.Approved
                  )
                  _ <-
                    Future.traverse(approvedApplications): application =>
                      subscribeAgentService.subscribeAgent(application).recover:
                        case ex => logger.warn(s"Subscription failed for ${application.applicationReference.value}: ${ex.getMessage}")
                yield ()
        yield Ok("Done")

  def subscribeToAgentApplication(applicationReference: ApplicationReference): Action[AnyContent] = Action
    .async:
      implicit request =>
        applicationForRiskingRepo
          .findByApplicationReference(applicationReference)
          .flatMap:
            case Some(applicationForRisking) =>
              subscribeAgentService.subscribeAgent(applicationForRisking).map: arn =>
                Ok(s"subscribed ok with arn: ${arn.value}")
            case None => Future.successful(NotFound(s"No application found for reference: ${applicationReference.value}"))

  private def makeApplicationForRisking(numberOfIndividuals: Int): ApplicationForRisking = ApplicationForRisking(
    applicationReference = agentReferenceGenerator.nextApplicationReference(),
    status = ApplicationForRiskingStatus.ReadyForSubmission,
    createdAt = Instant.now(),
    uploadedAt = None,
    fileName = None,
    agentDetails = AgentDetails(
      businessName = AgentBusinessName(
        agentBusinessName = generateRandomName(),
        otherAgentBusinessName = None
      ),
      telephoneNumber = Some(AgentTelephoneNumber(
        agentTelephoneNumber = "1234658979",
        otherAgentTelephoneNumber = None
      )),
      agentEmailAddress = Some(AgentVerifiedEmailAddress(
        emailAddress = AgentEmailAddress(
          agentEmailAddress = "agent@example.com",
          otherAgentEmailAddress = None
        ),
        isVerified = true
      )),
      agentCorrespondenceAddress = Some(
        AgentCorrespondenceAddress(
          addressLine1 = "23 Great Portland Street",
          addressLine2 = Some("London"),
          postalCode = Some("W1 8LT"),
          countryCode = "GB"
        )
      )
    ),
    applicantGroupId = GroupId("test-group-id"),
    applicantCredentials = Credentials(
      providerId = "test-provider-id",
      providerType = "test-provider-type"
    ),
    applicantName = ApplicantName(generateRandomName()),
    applicantPhone = Some(TelephoneNumber("1234658979")),
    applicantEmail = Some(EmailAddress("user@test.com")),
    entitySafeId = SafeId("X0TESTSAFEID0X"),
    entityType = BusinessType.Partnership.LimitedLiabilityPartnership,
    entityIdentifier = Utr("12345566"),
    crn = Some(Crn("12345566")),
    vrns = s"${generateRandomVrn()},${generateRandomVrn()}",
    payeRefs = s"${generateRandomPayeRef()},${generateRandomPayeRef()}",
    amlSupervisoryBody = AmlsCode("HMRC"),
    amlRegNumber = AmlsRegistrationNumber("11223344"),
    amlExpiryDate = Some(LocalDate.of(2030, 1, 1)),
    amlEvidence = None,
    individuals = createIndividualsList(numberOfIndividuals),
    failures = None
  )

  private def createIndividualsList(numberOfIndividuals: Int): List[IndividualForRisking] = (1 to numberOfIndividuals).map(_ => makeIndividual()).toList

  private def makeIndividual(): IndividualForRisking = IndividualForRisking(
    personReference = personReferenceGenerator.nextPersonReference(),
    status = ApplicationForRiskingStatus.ReadyForSubmission,
    vrns = s"${generateRandomVrn()},${generateRandomVrn()}",
    payeRefs = s"${generateRandomPayeRef()},${generateRandomPayeRef()}",
    companiesHouseName = None,
    companiesHouseDateOfBirth = None,
    providedName = IndividualName(generateRandomName()),
    providedDateOfBirth = Provided(generateRandomDateOfBirth()),
    nino = Some(IndividualNino.Provided(generateRandomNino())),
    saUtr = Some(IndividualSaUtr.Provided(generateRandomSaUtr())),
    phoneNumber = TelephoneNumber("01234567890"),
    email = EmailAddress("John@thomnet.com"),
    providedByApplicant = true,
    passedIV = true,
    failures = None
  )

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
