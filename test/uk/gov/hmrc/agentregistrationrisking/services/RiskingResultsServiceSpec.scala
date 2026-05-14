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

import org.mongodb.scala.SingleObservableFuture
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.repository.IndividualForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.AuditStubs
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.ObjectStoreStubs
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.SdesProxyStubs

class RiskingResultsServiceSpec
extends ISpec:

  // `auditing.enabled` defaults to false in ISpec, which makes DefaultAuditConnector short-circuit and post nothing.
  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "auditing.enabled" -> true
  )

  private lazy val service: RiskingResultsService = app.injector.instanceOf[RiskingResultsService]
  private lazy val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]
  private lazy val individualForRiskingRepo: IndividualForRiskingRepo = app.injector.instanceOf[IndividualForRiskingRepo]

  private given RequestHeader = tdAll.fakeBackendRequest

  // `failRecordArrayFileMatchingApp` carries records for these references; the DB records below are aligned to them
  // so the service can match and update them.
  private val applicationReference = tdAll.matchingApplicationReference
  private val personReference = tdAll.matchingPersonReference

  private val submitted = TdRiskingInstancesInStates.submittedForRisking
  private val application: ApplicationForRisking = submitted.application.copy(applicationReference = applicationReference)
  private val individual: IndividualForRisking = submitted.individual1.copy(
    personReference = personReference,
    applicationReference = applicationReference
  )

  override def beforeEach(): Unit =
    super.beforeEach()
    applicationForRiskingRepo.collection.drop().toFuture().futureValue
    individualForRiskingRepo.collection.drop().toFuture().futureValue
    ()

  "processResultsFiles" - {

    "sends a RiskingResponseEntity and a RiskingResponseIndividual audit event for each matched record in the results file" in:
      applicationForRiskingRepo.upsert(application).futureValue
      individualForRiskingRepo.upsert(individual).futureValue

      SdesProxyStubs.stubFindAvailableFiles(Seq(tdAll.testAvailableFile))
      ObjectStoreStubs.stubObjectStoreListObjects(processedFileNames = List.empty)
      ObjectStoreStubs.stubDownloadMinervaFile(tdAll.testDownloadUrl, tdAll.failRecordArrayFileMatchingApp)
      ObjectStoreStubs.stubObjectStoreUploadFromUrl(tdAll.testFileName)
      AuditStubs.stubAuditWrite()

      service.processResultsFiles().futureValue

      eventually:
        AuditStubs.verifyAuditSent(
          auditType = "RiskingResponseEntity",
          detail = Json.obj(
            "applicationReference" -> applicationReference.value,
            "riskingOutcome" -> "FixableFailure",
            "failures" -> Json.arr(Json.obj(
              "reasonCode" -> "3.2",
              "reasonDescription" -> "AML check failed due to suspicious activity"
            ))
          )
        )
        AuditStubs.verifyAuditSent(
          auditType = "RiskingResponseIndividual",
          detail = Json.obj(
            "applicationReference" -> applicationReference.value,
            "personReference" -> personReference.value,
            "riskingOutcome" -> "FixableFailure",
            "failures" -> Json.arr(Json.obj(
              "reasonCode" -> "4.1",
              "reasonDescription" -> "Outstanding returns overdue"
            ))
          )
        )

    "does not send any audit event when the results file references applications and individuals that are not in the database" in:
      // DB intentionally left empty — the file's references won't be found.
      SdesProxyStubs.stubFindAvailableFiles(Seq(tdAll.testAvailableFile))
      ObjectStoreStubs.stubObjectStoreListObjects(processedFileNames = List.empty)
      ObjectStoreStubs.stubDownloadMinervaFile(tdAll.testDownloadUrl, tdAll.failRecordArrayFileMatchingApp)
      ObjectStoreStubs.stubObjectStoreUploadFromUrl(tdAll.testFileName)
      AuditStubs.stubAuditWrite()

      service.processResultsFiles().futureValue

      AuditStubs.verifyNoAuditSent()
  }
