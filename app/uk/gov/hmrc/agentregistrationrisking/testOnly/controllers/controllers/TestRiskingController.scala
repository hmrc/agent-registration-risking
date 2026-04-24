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
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.agentdetails.*
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsLlp
import uk.gov.hmrc.agentregistration.shared.businessdetails.CompanyProfile
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantEmailAddress
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.individual.IndividualDateOfBirth.Provided
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsIdGenerator
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsIdGenerator
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState
import uk.gov.hmrc.agentregistrationrisking.action.Actions
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRiskingId
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRiskingIdGenerator
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRiskingId
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRiskingIdGenerator
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.runner.RiskingRunner
import uk.gov.hmrc.agentregistrationrisking.services.RiskingFileService
import uk.gov.hmrc.agentregistrationrisking.services.SubscribeAgentService
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.agentregistrationrisking.services.SdesProxyService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Clock
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
  individualForRiskingRepo: IndividualForRiskingRepo,
  applicationForRiskingIdGenerator: ApplicationForRiskingIdGenerator,
  individualForRiskingIdGenerator: IndividualForRiskingIdGenerator,
  individualProvidedDetailsIdGenerator: IndividualProvidedDetailsIdGenerator,
  agentReferenceGenerator: ApplicationReferenceGenerator,
  personReferenceGenerator: PersonReferenceGenerator,
  riskingFileService: RiskingFileService,
  riskingRunner: RiskingRunner,
  sdesProxyService: SdesProxyService,
  subscribeAgentService: SubscribeAgentService
)(using Clock)
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
        riskingFileService.getApplicationsReadyForRiskingWithIndividuals.map: applicationsWithIndividuals =>
          val riskingFile: String = riskingFileService.buildRiskingFileFrom(applicationsWithIndividuals)
          Ok(riskingFile)

  def createTestApplicationForRisking(numberOfIndividuals: Int): Action[AnyContent] = Action
    .async:
      implicit request =>
        val now = Instant.now(summon[Clock])
        val appId = applicationForRiskingIdGenerator.nextApplicationId()
        val applicationForRisking = makeApplicationForRisking(appId, now)
        val individuals = createIndividualsList(
          numberOfIndividuals,
          appId,
          now
        )
        for
          _ <- applicationForRiskingRepo.upsert(applicationForRisking)
          _ <- Future.traverse(individuals)(individualForRiskingRepo.upsert)
        yield Ok(Json.obj("applicationForRiskingId" -> appId.value))

  def downloadAvailableResultsFiles: Action[AnyContent] = Action
    .async:
      implicit request =>
        sdesProxyService.retrieveAndProcessResultsFiles.map(result => Ok(result.toString()))

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

  private def makeApplicationForRisking(
    appId: ApplicationForRiskingId,
    now: Instant
  ): ApplicationForRisking =
    val agentApplication = AgentApplicationLlp(
      _id = AgentApplicationId(appId.value),
      applicationReference = agentReferenceGenerator.generateApplicationReference(),
      internalUserId = InternalUserId("test-internal-user-id"),
      applicantCredentials = Credentials(
        providerId = "test-provider-id",
        providerType = "test-provider-type"
      ),
      linkId = LinkId("test-link-id"),
      groupId = GroupId("test-group-id"),
      createdAt = now,
      submittedAt = Some(now),
      applicationState = ApplicationState.SentForRisking,
      userRole = Some(UserRole.Partner),
      businessDetails = Some(BusinessDetailsLlp(
        companyProfile = CompanyProfile(
          companyNumber = Crn("12345566"),
          companyName = "Test LLP",
          dateOfIncorporation = Some(LocalDate.now()),
          unsanitisedCHROAddress = None
        ),
        saUtr = SaUtr("12345566"),
        safeId = SafeId("X0TESTSAFEID0X")
      )),
      applicantContactDetails = Some(ApplicantContactDetails(
        applicantName = ApplicantName(generateRandomName()),
        telephoneNumber = Some(TelephoneNumber("1234658979")),
        applicantEmailAddress = Some(ApplicantEmailAddress(EmailAddress("user@test.com"), isVerified = true))
      )),
      amlsDetails = Some(AmlsDetails(
        supervisoryBody = AmlsCode("HMRC"),
        amlsRegistrationNumber = Some(AmlsRegistrationNumber("11223344")),
        amlsEvidence = None
      )),
      agentDetails = Some(AgentDetails(
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
      )),
      refusalToDealWithCheckResult = Some(CheckResult.Pass),
      companyStatusCheckResult = Some(CheckResult.Pass),
      hmrcStandardForAgentsAgreed = StateOfAgreement.Agreed,
      numberOfIndividuals = None,
      hasOtherRelevantIndividuals = Some(false),
      vrns = Some(List(Vrn(generateRandomVrn()), Vrn(generateRandomVrn()))),
      payeRefs = Some(List(PayeRef(generateRandomPayeRef()), PayeRef(generateRandomPayeRef())))
    )
    ApplicationForRisking(
      _id = appId,
      agentApplication = agentApplication,
      createdAt = now,
      lastUpdatedAt = now,
      riskingFileId = None,
      failures = None,
      isSubscribed = false,
      isEmailSent = false
    )

  private def createIndividualsList(
    numberOfIndividuals: Int,
    appId: ApplicationForRiskingId,
    now: Instant
  ): List[IndividualForRisking] = (1 to numberOfIndividuals).map(_ => makeIndividual(appId, now)).toList

  private def makeIndividual(
    appId: ApplicationForRiskingId,
    now: Instant
  ): IndividualForRisking =
    val providedDetails = IndividualProvidedDetails(
      _id = individualProvidedDetailsIdGenerator.nextIndividualProvidedDetailsId(),
      personReference = personReferenceGenerator.nextPersonReference(),
      individualName = IndividualName(generateRandomName()),
      isPersonOfControl = true,
      internalUserId = None,
      createdAt = now,
      providedDetailsState = ProvidedDetailsState.Finished,
      agentApplicationId = AgentApplicationId(appId.value),
      individualDateOfBirth = Some(Provided(generateRandomDateOfBirth())),
      telephoneNumber = Some(TelephoneNumber("01234567890")),
      emailAddress = None,
      individualNino = Some(IndividualNino.Provided(generateRandomNino())),
      individualSaUtr = Some(IndividualSaUtr.Provided(generateRandomSaUtr())),
      hmrcStandardForAgentsAgreed = StateOfAgreement.Agreed,
      hasApprovedApplication = Some(true),
      vrns = Some(List(Vrn(generateRandomVrn()), Vrn(generateRandomVrn()))),
      payeRefs = Some(List(PayeRef(generateRandomPayeRef()), PayeRef(generateRandomPayeRef()))),
      passedIv = Some(true)
    )
    IndividualForRisking(
      _id = individualForRiskingIdGenerator.nextIndividualId(),
      applicationForRiskingId = appId,
      individualProvidedDetails = providedDetails,
      createdAt = now,
      lastUpdatedAt = now,
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
