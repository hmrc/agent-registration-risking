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
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcome
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationWithIndividuals
import uk.gov.hmrc.agentregistrationrisking.model.RiskingResult
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ApplicationForRiskingService @Inject() (
  applicationForRiskingRepo: ApplicationForRiskingRepo,
  individualForRiskingRepo: IndividualForRiskingRepo
)(using
  ExecutionContext,
  Clock
)
extends RequestAwareLogging:

  def getApplicationWithIndividuals(applicationReference: ApplicationReference): Future[Option[ApplicationWithIndividuals]] =
    for
      maybeApplication: Option[ApplicationForRisking] <- applicationForRiskingRepo.findById(applicationReference)
      individualForRisking: Seq[IndividualForRisking] <- individualForRiskingRepo.findByApplicationReference(applicationReference)
      applicationWithIndividuals: Option[ApplicationWithIndividuals] = maybeApplication.map(ApplicationWithIndividuals(_, individualForRisking))
    yield applicationWithIndividuals

  import cats.data.OptionT
  import cats.implicits.*

  def getApplicationWithIndividuals(personReference: PersonReference): Future[Option[ApplicationWithIndividuals]] =
    (for
      individual: IndividualForRisking <- OptionT(individualForRiskingRepo.findById(personReference))
      application: ApplicationForRisking <- OptionT(applicationForRiskingRepo.findById(individual.applicationReference))
      individuals: Seq[IndividualForRisking] <- OptionT.liftF(individualForRiskingRepo.findByApplicationReference(individual.applicationReference))
      applicationWithIndividuals: ApplicationWithIndividuals = ApplicationWithIndividuals(application, individuals)
    yield applicationWithIndividuals).value
