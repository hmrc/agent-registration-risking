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

package uk.gov.hmrc.agentregistrationrisking.controllers

import play.api.mvc.Action
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedCompany
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.Crn
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationForRiskingStatus
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.risking.SubmitForRiskingRequest
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistration.shared.util.OptionalListExtensions.transformToCommaSeparatedString
import uk.gov.hmrc.agentregistrationrisking.action.Actions
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.util.chaining.scalaUtilChainingOps

@Singleton()
class SubmitForRiskingController @Inject() (
  actions: Actions,
  cc: ControllerComponents,
  applicationForRiskingRepo: ApplicationForRiskingRepo
)
extends BackendController(cc):

  def submitForRisking(): Action[SubmitForRiskingRequest] =
    actions
      .authorised
      .async(parse.json[SubmitForRiskingRequest]):
        implicit request =>
          request
            .body
            .toApplicationForRisking
            .pipe(applicationForRiskingRepo.upsert)
            .map(_ => Created)

  extension (individual: IndividualProvidedDetails)
    def toIndividualsForRisking: IndividualForRisking = IndividualForRisking(
      personReference = individual.personReference,
      status = ApplicationForRiskingStatus.ReadyForSubmission,
      vrns = transformToCommaSeparatedString(individual.vrns.map(_.map(_.value))),
      payeRefs = transformToCommaSeparatedString(individual.payeRefs.map(_.map(_.value))),
      companiesHouseName = None, // We don't currently store the name retrieved from companies house
      companiesHouseDateOfBirth = None, // As above
      providedName = individual.individualName,
      providedDateOfBirth = individual.getDateOfBirth,
      nino = individual.individualNino,
      saUtr = individual.individualSaUtr,
      phoneNumber = individual.getTelephoneNumber,
      email = individual.getEmailAddress.emailAddress,
      providedByApplicant = false, // It's not currently possible for the applicant to provide details, only individuals can do it themselves
      passedIV = individual.getPassedIv,
      failures = None
    )

  extension (individual: IndividualProvidedDetails)
    def getPassedIv: Boolean = individual.passedIv.getOrThrowExpectedDataMissing("Passed IV result")

  extension (submitForRiskingRequest: SubmitForRiskingRequest)

    def toApplicationForRisking: ApplicationForRisking =
      val application = submitForRiskingRequest.agentApplication
      ApplicationForRisking(
        applicationReference = ApplicationReference(application.agentApplicationId.value),
        status = ApplicationForRiskingStatus.ReadyForSubmission,
        createdAt = Instant.now(),
        uploadedAt = None,
        fileName = None,
        agentDetails = application.getAgentDetails,
        applicantGroupId = application.groupId,
        applicantCredentials = application.applicantCredentials,
        applicantName = application.getApplicantContactDetails.applicantName,
        applicantPhone = application.getApplicantContactDetails.telephoneNumber,
        applicantEmail = application.getApplicantContactDetails.applicantEmailAddress.map(_.emailAddress),
        entitySafeId = application.getSafeId,
        entityType = application.businessType,
        entityIdentifier = application.getUtr,
        crn = getMaybeCrn(application),
        vrns = transformToCommaSeparatedString(application.vrns.map(_.map(_.value))),
        payeRefs = transformToCommaSeparatedString(application.payeRefs.map(_.map(_.value))),
        amlSupervisoryBody = application.getAmlsDetails.supervisoryBody,
        amlRegNumber = application.getAmlsDetails.getRegistrationNumber,
        amlExpiryDate = None, // we don't capture the AML expiry date in the application
        amlEvidence = application.getAmlsDetails.amlsEvidence,
        individuals = submitForRiskingRequest.individuals.map(_.toIndividualsForRisking),
        failures = None
      )

    private def getMaybeCrn(agentApplication: AgentApplication): Option[Crn] =
      agentApplication match {
        case a: AgentApplicationLimitedCompany => Some(a.getBusinessDetails.companyProfile.companyNumber)
        case a: AgentApplicationLimitedPartnership => Some(a.getBusinessDetails.companyProfile.companyNumber)
        case a: AgentApplicationLlp => Some(a.getBusinessDetails.companyProfile.companyNumber)
        case a: AgentApplicationScottishLimitedPartnership => Some(a.getBusinessDetails.companyProfile.companyNumber)
        case _ => None
      }
