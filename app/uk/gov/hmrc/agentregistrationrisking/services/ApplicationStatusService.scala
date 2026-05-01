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

import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcome
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationrisking.model.*
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationrisking.services.RiskingOutcomeHelper._

import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ApplicationStatusService @Inject() (
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  individualForRiskingRepo: IndividualForRiskingRepo
)(using
  ExecutionContext,
  Clock
)
extends RequestAwareLogging:

  def findApprovedReadyToSubscribe(): Future[Seq[ApplicationForRisking]] = getApplicationsPendingActionWithIndividuals
    .map(filterApprovedApplicationsWithIndividuals)
    .map(_.map(_.application))

  def findNonFixableReadyForFailureEmail(): Future[Seq[ApplicationWithIndividuals]] = getApplicationsPendingActionWithIndividuals
    .map(filterNonFixableApplicationsWithIndividuals)

  private def getApplicationsPendingActionWithIndividuals: Future[Seq[ApplicationWithIndividuals]] =
    for
      applications <- applicationForRiskingRepo.findApplicationsPendingAction()
      individuals <- individualForRiskingRepo.findByApplicationReferences(applications.map(_.applicationReference))
    yield ApplicationWithIndividuals
      .merge(applications, individuals)
      .filter(_.individuals.forall(_.failures.isDefined))

  private def filterApprovedApplicationsWithIndividuals(applicationsWithIndividuals: Seq[ApplicationWithIndividuals]): Seq[ApplicationWithIndividuals] =
    applicationsWithIndividuals.filter: appWithIndividuals =>
      RiskingOutcomeHelper
        .computeRiskingOutcome(appWithIndividuals)
        .exists(_ === RiskingOutcome.Approved)

  private def filterNonFixableApplicationsWithIndividuals(applicationsWithIndividuals: Seq[ApplicationWithIndividuals]): Seq[ApplicationWithIndividuals] =
    applicationsWithIndividuals
      .filter: appWithIndividuals =>
        RiskingOutcomeHelper
          .computeRiskingOutcome(appWithIndividuals)
          .exists(_ === RiskingOutcome.FailedNonFixable)
      .map: appWithIndividuals =>
        val nonFixableIndividuals = appWithIndividuals.individuals.filter: individual =>
          individual.failures.exists(_.outcome === RiskingOutcome.FailedNonFixable)
        ApplicationWithIndividuals(appWithIndividuals.application, nonFixableIndividuals)

  private def filterFixableApplicationsWithIndividuals(applicationsWithIndividuals: Seq[ApplicationWithIndividuals]): Seq[ApplicationWithIndividuals] =
    applicationsWithIndividuals
      .filter: appWithIndividuals =>
        RiskingOutcomeHelper
          .computeRiskingOutcome(appWithIndividuals)
          .exists(_ === RiskingOutcome.FailedFixable)
      .map: appWithIndividuals =>
        val fixableIndividuals = appWithIndividuals.individuals.filter: individual =>
          individual.failures.exists(_.outcome === RiskingOutcome.FailedFixable)
        ApplicationWithIndividuals(appWithIndividuals.application, fixableIndividuals)
