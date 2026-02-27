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

import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.agentregistration.shared.AmlsCode
import uk.gov.hmrc.agentregistration.shared.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.Crn
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.individual.IndividualDateOfBirth.Provided
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistrationrisking.action.Actions
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRiskingStatus
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationReferenceGenerator
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.PersonReferenceGenerator
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton()
class TestRiskingController @Inject() (
  cc: ControllerComponents,
  actions: Actions,
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  agentReferenceGenerator: ApplicationReferenceGenerator,
  personReferenceGenerator: PersonReferenceGenerator
)
extends BackendController(cc):

  given ExecutionContext = controllerComponents.executionContext

  def createTestApplicationForRisking: Action[AnyContent] = Action
    .async:
      implicit request =>
        val applicationForRisking: ApplicationForRisking = makeApplicationForRisking()
        applicationForRiskingRepo
          .upsert(applicationForRisking)
          .map(_ => Ok(Json.obj("applicationReference" -> applicationForRisking.applicationReference.value)))

  private def makeApplicationForRisking(): ApplicationForRisking = ApplicationForRisking(
    applicationReference = agentReferenceGenerator.nextApplicationReference(),
    status = ApplicationForRiskingStatus.ReadyForSubmission,
    createdAt = Instant.now(),
    uploadedAt = None,
    fileName = None,
    applicantName = ApplicantName("Bob Ross"),
    applicantPhone = Some(TelephoneNumber("1234658979")),
    applicantEmail = Some(EmailAddress("user@test.com")),
    entityType = BusinessType.Partnership.LimitedLiabilityPartnership,
    entityIdentifier = Utr("12345566"),
    crn = Some(Crn("12345566")),
    vrns = "22345566,22345567",
    payeRefs = "32345566,32345567",
    amlSupervisoryBody = AmlsCode("HMRC"),
    amlRegNumber = AmlsRegistrationNumber("11223344"),
    amlExpiryDate = Some(LocalDate.of(2030, 1, 1)),
    amlEvidence = None,
    individuals = List(makeIndividual(), makeIndividual()),
    failures = None
  )

  private def makeIndividual(): IndividualForRisking = IndividualForRisking(
    personReference = personReferenceGenerator.nextPersonReference(),
    status = ApplicationForRiskingStatus.ReadyForSubmission,
    vrns = "55345566,55345567",
    payeRefs = "66345566,66345567",
    companiesHouseName = Some("John Thompson"),
    companiesHouseDateOfBirth = Some(LocalDate.of(1980, 1, 1)),
    providedName = IndividualName("John Thompson"),
    providedDateOfBirth = Provided(LocalDate.of(1980, 1, 1)),
    nino = Some(IndividualNino.Provided(Nino("AA123456A"))),
    saUtr = Some(IndividualSaUtr.Provided(SaUtr("1234567890"))),
    phoneNumber = TelephoneNumber("01234567890"),
    email = EmailAddress("John@thomnet.com"),
    providedByApplicant = true,
    passedIV = true,
    failures = None
  )
