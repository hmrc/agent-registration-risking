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

package uk.gov.hmrc.agentregistrationrisking.audit

import play.api.libs.json.OWrites
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistrationrisking.model.Failure
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.RiskingResult
import uk.gov.hmrc.agentregistrationrisking.services.RiskingOutcomeHelper.*
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationrisking.util.RequestSupport.hc
import uk.gov.hmrc.play.audit.DefaultAuditConnector

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class AuditService @Inject() (auditConnector: DefaultAuditConnector)(using ec: ExecutionContext)
extends RequestAwareLogging:

  def sendRiskingResponseEntityEvent(result: RiskingResult.ForEntity)(using RequestHeader): Unit =
    val event = RiskingResponseEntity(
      applicationReference = result.applicationReference,
      riskingOutcome = AuditOutcome.fromRiskingOutcome(result.failures.outcomeForEntity),
      failures = toFailureDetails(result.rawFailures)
    )
    send(event)

  def sendRiskingResponseIndividualEvent(
    individual: IndividualForRisking,
    result: RiskingResult.ForIndividual
  )(using RequestHeader): Unit =
    val event = RiskingResponseIndividual(
      applicationReference = individual.applicationReference,
      personReference = individual.personReference,
      riskingOutcome = AuditOutcome.fromRiskingOutcome(result.failures.outcome),
      failures = toFailureDetails(result.rawFailures)
    )
    send(event)

  private def send[E <: AuditEvent](event: E)(using
    RequestHeader,
    OWrites[E]
  ): Unit =
    logger.info(s"Auditing ${event.auditType}")
    auditConnector.sendExplicitAudit(auditType = event.auditType, detail = event)

  private def toFailureDetails(rawFailures: List[Failure]): Option[List[FailureDetail]] =
    if rawFailures.isEmpty then None
    else Some(rawFailures.map(f => FailureDetail(reasonCode = f.reasonCode, reasonDescription = f.reasonDescription)))
