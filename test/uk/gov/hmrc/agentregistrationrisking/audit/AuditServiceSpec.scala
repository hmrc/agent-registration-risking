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

import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistrationrisking.model.Failure
import uk.gov.hmrc.agentregistrationrisking.model.RiskingOutcome
import uk.gov.hmrc.agentregistrationrisking.model.RiskingResult
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdFailures
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.AuditStubs

class AuditServiceSpec
extends ISpec:

  // `auditing.enabled` defaults to false in ISpec, which makes DefaultAuditConnector short-circuit and post nothing.
  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "auditing.enabled" -> true
  )

  private lazy val auditService: AuditService = app.injector.instanceOf[AuditService]
  private given RequestHeader = tdAll.fakeBackendRequest

  private val td = tdAll.tdRiskingInstancesInStates.approved
  private val applicationReference = td.application.applicationReference
  private val individual = td.individual1

  "sendRiskingResponseEntityEvent" - {

    "sends RiskingResponseEntity with Success outcome and no failures when the entity has no failures" in:
      AuditStubs.stubAuditWrite()

      auditService.sendRiskingResponseEntityEvent(
        RiskingResult.ForEntity(
          applicationReference,
          failures = List.empty,
          rawFailures = List.empty
        )
      )

      eventually:
        AuditStubs.verifyAuditSent(
          auditType = "RiskingResponseEntity",
          detail = Json.obj(
            "applicationReference" -> applicationReference.value,
            "riskingOutcome" -> "Success"
          )
        )

    "sends RiskingResponseEntity with NonFixableFailure outcome and failure details when the entity has a non-fixable failure" in:
      AuditStubs.stubAuditWrite()

      auditService.sendRiskingResponseEntityEvent(
        RiskingResult.ForEntity(
          applicationReference,
          failures = List(TdFailures.entityFailures.nonFixable1),
          rawFailures = List(Failure(
            reasonCode = "8.1",
            reasonDescription = "Connected to a tax avoidance scheme",
            checkId = "8",
            checkDescription = "Anti-avoidance",
            additionalInfo = None
          ))
        )
      )

      eventually:
        AuditStubs.verifyAuditSent(
          auditType = "RiskingResponseEntity",
          detail = Json.obj(
            "applicationReference" -> applicationReference.value,
            "riskingOutcome" -> "NonFixableFailure",
            "failures" -> Json.arr(Json.obj(
              "reasonCode" -> "8.1",
              "reasonDescription" -> "Connected to a tax avoidance scheme"
            ))
          )
        )
  }

  "sendRiskingResponseIndividualEvent" - {

    "sends RiskingResponseIndividual with Success outcome and no failures when the individual has no failures" in:
      AuditStubs.stubAuditWrite()

      auditService.sendRiskingResponseIndividualEvent(
        individual,
        RiskingResult.ForIndividual(
          individual.personReference,
          failures = List.empty,
          rawFailures = List.empty
        )
      )

      eventually:
        AuditStubs.verifyAuditSent(
          auditType = "RiskingResponseIndividual",
          detail = Json.obj(
            "applicationReference" -> individual.applicationReference.value,
            "personReference" -> individual.personReference.value,
            "riskingOutcome" -> "Success"
          )
        )

    "sends RiskingResponseIndividual with FixableFailure outcome and failure details when the individual has a fixable failure" in:
      AuditStubs.stubAuditWrite()

      auditService.sendRiskingResponseIndividualEvent(
        individual,
        RiskingResult.ForIndividual(
          individual.personReference,
          failures = List(TdFailures.individualFailures.fixable1),
          rawFailures = List(Failure(
            reasonCode = "4.1",
            reasonDescription = "One or more overdue SA returns",
            checkId = "4",
            checkDescription = "Overdue returns",
            additionalInfo = None
          ))
        )
      )

      eventually:
        AuditStubs.verifyAuditSent(
          auditType = "RiskingResponseIndividual",
          detail = Json.obj(
            "applicationReference" -> individual.applicationReference.value,
            "personReference" -> individual.personReference.value,
            "riskingOutcome" -> "FixableFailure",
            "failures" -> Json.arr(Json.obj(
              "reasonCode" -> "4.1",
              "reasonDescription" -> "One or more overdue SA returns"
            ))
          )
        )
  }

  "sendRiskingDeterminationEvent" - {

    "sends RiskingDetermination with Success when the overall outcome is Approved" in:
      AuditStubs.stubAuditWrite()

      auditService.sendRiskingDeterminationEvent(applicationReference, RiskingOutcome.Approved)

      eventually:
        AuditStubs.verifyAuditSent(
          auditType = "RiskingDetermination",
          detail = Json.obj(
            "applicationReference" -> applicationReference.value,
            "determination" -> "Success"
          )
        )

    "sends RiskingDetermination with NonFixableFailure when the overall outcome is FailedNonFixable" in:
      AuditStubs.stubAuditWrite()

      auditService.sendRiskingDeterminationEvent(applicationReference, RiskingOutcome.FailedNonFixable)

      eventually:
        AuditStubs.verifyAuditSent(
          auditType = "RiskingDetermination",
          detail = Json.obj(
            "applicationReference" -> applicationReference.value,
            "determination" -> "NonFixableFailure"
          )
        )

    "sends RiskingDetermination with FixableFailure when the overall outcome is FailedFixable" in:
      AuditStubs.stubAuditWrite()

      auditService.sendRiskingDeterminationEvent(applicationReference, RiskingOutcome.FailedFixable)

      eventually:
        AuditStubs.verifyAuditSent(
          auditType = "RiskingDetermination",
          detail = Json.obj(
            "applicationReference" -> applicationReference.value,
            "determination" -> "FixableFailure"
          )
        )
  }
