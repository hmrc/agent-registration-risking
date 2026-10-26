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

import com.softwaremill.quicklens.modify
import org.mongodb.scala.SingleObservableFuture
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.Arn
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.EnrolmentFailure
import uk.gov.hmrc.agentregistrationrisking.repository.ApplicationForRiskingRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdRiskingInstancesInStates
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.AuditStubs
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.EnrolmentStoreProxyStubs

class AlreadyAllocatedToGroupRecoveryServiceSpec
extends ISpec:

  // `auditing.enabled` defaults to false in ISpec, which makes DefaultAuditConnector short-circuit and post nothing.
  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "auditing.enabled" -> true
  )

  private val alreadyAllocatedToGroupRecoveryService: AlreadyAllocatedToGroupRecoveryService = app.injector.instanceOf[AlreadyAllocatedToGroupRecoveryService]
  private val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]

  private given RequestHeader = tdAll.fakeBackendRequest

  override def beforeEach(): Unit =
    super.beforeEach()
    applicationForRiskingRepo.collection.drop().toFuture.futureValue
    ()

  private val parkedApplication: ApplicationForRisking = TdRiskingInstancesInStates.approvedAfterOutcomeParkedAlreadyAllocatedToGroup.application
  private val arn: Arn = parkedApplication.applicationData.getArn
  private val differentArn: Arn = tdAll.arn
  private val enrolmentKey: String = s"HMRC-AS-AGENT~AgentReferenceNumber~${arn.value}"
  private val providerId: String = parkedApplication.applicationData.applicantCredentials.providerId

  private def persisted(application: ApplicationForRisking): ApplicationForRisking =
    applicationForRiskingRepo
      .findById(application.applicationReference)
      .futureValue
      .value

  private def verifyNoAccountCreatedAudit(): Unit = AuditStubs.verifyAuditSent(
    auditType = "CreateAgentServicesAccount",
    detail = Json.obj(
      "applicationReference" -> parkedApplication.applicationReference.value,
      "agentReferenceNumber" -> arn.value
    ),
    count = 0
  )

  "processRecoveries" - {

    "marks the application subscribed and clears the enrolment failure when the applicant credential already holds the enrolment" in:
      AuditStubs.stubAuditWrite()
      EnrolmentStoreProxyStubs.stubQueryPrincipalEnrolmentsForUser(providerId, arn.value)
      applicationForRiskingRepo.upsert(parkedApplication).futureValue

      alreadyAllocatedToGroupRecoveryService.processRecoveries().futureValue

      EnrolmentStoreProxyStubs.verifyQueryPrincipalEnrolmentsForUser()
      EnrolmentStoreProxyStubs.verifyAssignEnrolmentToUser(count = 0)
      persisted(parkedApplication).isSubscribed shouldBe true
      persisted(parkedApplication).enrolmentFailure shouldBe None

    "parks the application as GroupEnrolledForDifferentArn when the applicant credential holds the enrolment for a different ARN" in:
      AuditStubs.stubAuditWrite()
      EnrolmentStoreProxyStubs.stubQueryPrincipalEnrolmentsForUser(providerId, differentArn.value)
      applicationForRiskingRepo.upsert(parkedApplication).futureValue

      alreadyAllocatedToGroupRecoveryService.processRecoveries().futureValue

      EnrolmentStoreProxyStubs.verifyAssignEnrolmentToUser(count = 0)
      verifyNoAccountCreatedAudit()
      persisted(parkedApplication).isSubscribed shouldBe false
      persisted(parkedApplication).enrolmentFailure shouldBe Some(EnrolmentFailure.GroupEnrolledForDifferentArn)

    "assigns the enrolment to the applicant credential and marks the application subscribed when the credential holds no enrolment" in:
      AuditStubs.stubAuditWrite()
      EnrolmentStoreProxyStubs.stubQueryPrincipalEnrolmentsForUserEmptyList(providerId)
      EnrolmentStoreProxyStubs.stubAssignEnrolmentToUser(providerId, enrolmentKey)
      applicationForRiskingRepo.upsert(parkedApplication).futureValue

      alreadyAllocatedToGroupRecoveryService.processRecoveries().futureValue

      EnrolmentStoreProxyStubs.verifyQueryPrincipalEnrolmentsForUser()
      EnrolmentStoreProxyStubs.verifyAssignEnrolmentToUser()
      persisted(parkedApplication).isSubscribed shouldBe true
      persisted(parkedApplication).enrolmentFailure shouldBe None

    "audits the created agent services account after assigning the enrolment" in:
      AuditStubs.stubAuditWrite()
      EnrolmentStoreProxyStubs.stubQueryPrincipalEnrolmentsForUserEmptyList(providerId)
      EnrolmentStoreProxyStubs.stubAssignEnrolmentToUser(providerId, enrolmentKey)
      applicationForRiskingRepo.upsert(parkedApplication).futureValue

      alreadyAllocatedToGroupRecoveryService.processRecoveries().futureValue

      eventually:
        AuditStubs.verifyAuditSent(
          auditType = "CreateAgentServicesAccount",
          detail = Json.obj(
            "applicationReference" -> parkedApplication.applicationReference.value,
            "agentReferenceNumber" -> arn.value
          )
        )

    "parks the application as CredentialDoesNotExist when the enrolment store does not know the applicant credential" in:
      AuditStubs.stubAuditWrite()
      EnrolmentStoreProxyStubs.stubQueryPrincipalEnrolmentsForUserNotFound(providerId)
      applicationForRiskingRepo.upsert(parkedApplication).futureValue

      alreadyAllocatedToGroupRecoveryService.processRecoveries().futureValue

      EnrolmentStoreProxyStubs.verifyAssignEnrolmentToUser(count = 0)
      verifyNoAccountCreatedAudit()
      persisted(parkedApplication).isSubscribed shouldBe false
      persisted(parkedApplication).enrolmentFailure shouldBe Some(EnrolmentFailure.CredentialDoesNotExist)

    "parks the application as GroupEnrolledForDifferentArn when the enrolment store refuses to assign the enrolment to the applicant credential" in:
      AuditStubs.stubAuditWrite()
      EnrolmentStoreProxyStubs.stubQueryPrincipalEnrolmentsForUserEmptyList(providerId)
      EnrolmentStoreProxyStubs.stubAssignEnrolmentToUserForbidden(providerId, enrolmentKey)
      applicationForRiskingRepo.upsert(parkedApplication).futureValue

      alreadyAllocatedToGroupRecoveryService.processRecoveries().futureValue

      EnrolmentStoreProxyStubs.verifyAssignEnrolmentToUser()
      verifyNoAccountCreatedAudit()
      persisted(parkedApplication).isSubscribed shouldBe false
      persisted(parkedApplication).enrolmentFailure shouldBe Some(EnrolmentFailure.GroupEnrolledForDifferentArn)

    "leaves the application alone when the enrolment store fails, so the next run retries it" in:
      AuditStubs.stubAuditWrite()
      EnrolmentStoreProxyStubs.stubQueryPrincipalEnrolmentsForUserFailure(providerId)
      applicationForRiskingRepo.upsert(parkedApplication).futureValue

      alreadyAllocatedToGroupRecoveryService.processRecoveries().futureValue

      EnrolmentStoreProxyStubs.verifyAssignEnrolmentToUser(count = 0)
      verifyNoAccountCreatedAudit()
      persisted(parkedApplication).isSubscribed shouldBe false
      persisted(parkedApplication).enrolmentFailure shouldBe Some(EnrolmentFailure.AlreadyAllocatedToGroup)

    "parks the application as ArnNotRecorded without calling the enrolment store when it has no ARN" in:
      val parkedWithoutArn: ApplicationForRisking = parkedApplication.modify(_.applicationData.arn).setTo(None)
      AuditStubs.stubAuditWrite()
      applicationForRiskingRepo.upsert(parkedWithoutArn).futureValue

      alreadyAllocatedToGroupRecoveryService.processRecoveries().futureValue

      EnrolmentStoreProxyStubs.verifyQueryPrincipalEnrolmentsForUser(count = 0)
      verifyNoAccountCreatedAudit()
      persisted(parkedWithoutArn).isSubscribed shouldBe false
      persisted(parkedWithoutArn).enrolmentFailure shouldBe Some(EnrolmentFailure.ArnNotRecorded)

    "ignores an application that is not parked with AlreadyAllocatedToGroup" in:
      applicationForRiskingRepo.upsert(TdRiskingInstancesInStates.approvedAfterOutcome.application).futureValue

      alreadyAllocatedToGroupRecoveryService.processRecoveries().futureValue

      EnrolmentStoreProxyStubs.verifyQueryPrincipalEnrolmentsForUser(count = 0)

    "ignores an application parked with a terminal enrolment failure" in:
      applicationForRiskingRepo.upsert(TdRiskingInstancesInStates.approvedAfterOutcomeParkedGroupEnrolledForDifferentArn.application).futureValue

      alreadyAllocatedToGroupRecoveryService.processRecoveries().futureValue

      EnrolmentStoreProxyStubs.verifyQueryPrincipalEnrolmentsForUser(count = 0)
  }
