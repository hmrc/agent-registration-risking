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
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.HipStubs

class SubscriptionServiceSpec
extends ISpec:

  // `auditing.enabled` defaults to false in ISpec, which makes DefaultAuditConnector short-circuit and post nothing.
  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "auditing.enabled" -> true
  )

  private val subscriptionService: SubscriptionService = app.injector.instanceOf[SubscriptionService]
  private val applicationForRiskingRepo: ApplicationForRiskingRepo = app.injector.instanceOf[ApplicationForRiskingRepo]

  private given RequestHeader = tdAll.fakeBackendRequest

  override def beforeEach(): Unit =
    super.beforeEach()
    applicationForRiskingRepo.collection.drop().toFuture.futureValue
    ()

  /** Approved and not yet subscribed - the state `findReadyToBeSubscribed` looks for. The fixture already carries an ARN, which is the "already subscribed at
    * HIP" case, so the ARN is cleared here and restored in the one test that needs it.
    */
  private val approvedNotSubscribed: ApplicationForRisking = TdRiskingInstancesInStates.approvedAfterOutcome.application
  private val readyToSubscribe: ApplicationForRisking = approvedNotSubscribed.modify(_.applicationData.arn).setTo(None)

  private val arn: Arn = tdAll.arn
  private val enrolmentKey: String = enrolmentKeyFor(arn)
  private val groupId: String = readyToSubscribe.applicationData.groupId.value

  private def enrolmentKeyFor(arn: Arn): String = s"HMRC-AS-AGENT~AgentReferenceNumber~${arn.value}"

  private def stubSubscribeAndEnrol(): Unit =
    AuditStubs.stubAuditWrite()
    HipStubs.stubSubscribeToAgentServices(readyToSubscribe.applicationData.safeId, arn.value)
    EnrolmentStoreProxyStubs.stubAddKnownFacts(enrolmentKey)
    EnrolmentStoreProxyStubs.stubAllocateEnrolmentToGroup(groupId, enrolmentKey)
    ()

  private def persisted(application: ApplicationForRisking): ApplicationForRisking =
    applicationForRiskingRepo
      .findById(application.applicationReference)
      .futureValue
      .value

  "processSubscriptions" - {

    "subscribes the agent, allocates the enrolment and marks the application subscribed" in:
      stubSubscribeAndEnrol()
      applicationForRiskingRepo.upsert(readyToSubscribe).futureValue

      subscriptionService.processSubscriptions().futureValue

      HipStubs.verifySubscribeToAgentServices()
      EnrolmentStoreProxyStubs.verifyAddKnownFacts()
      EnrolmentStoreProxyStubs.verifyAllocateEnrolmentToGroup()
      persisted(readyToSubscribe).isSubscribed shouldBe true
      persisted(readyToSubscribe).enrolmentFailure shouldBe None

    "audits the created agent services account" in:
      stubSubscribeAndEnrol()
      applicationForRiskingRepo.upsert(readyToSubscribe).futureValue

      subscriptionService.processSubscriptions().futureValue

      eventually:
        AuditStubs.verifyAuditSent(
          auditType = "CreateAgentServicesAccount",
          detail = Json.obj(
            "applicationReference" -> readyToSubscribe.applicationReference.value,
            "agentReferenceNumber" -> arn.value
          )
        )

    "does not call HIP again when the application already holds an ARN, but still allocates the enrolment" in:
      val existingArn: Arn = approvedNotSubscribed.applicationData.getArn
      AuditStubs.stubAuditWrite()
      EnrolmentStoreProxyStubs.stubAddKnownFacts(enrolmentKeyFor(existingArn))
      EnrolmentStoreProxyStubs.stubAllocateEnrolmentToGroup(groupId, enrolmentKeyFor(existingArn))
      applicationForRiskingRepo.upsert(approvedNotSubscribed).futureValue

      subscriptionService.processSubscriptions().futureValue

      HipStubs.verifySubscribeToAgentServices(count = 0)
      EnrolmentStoreProxyStubs.verifyAllocateEnrolmentToGroup()
      persisted(approvedNotSubscribed).isSubscribed shouldBe true

    "records the enrolment failure and leaves the application unsubscribed when the enrolment cannot be allocated" in:
      AuditStubs.stubAuditWrite()
      HipStubs.stubSubscribeToAgentServices(readyToSubscribe.applicationData.safeId, arn.value)
      EnrolmentStoreProxyStubs.stubAddKnownFacts(enrolmentKey)
      EnrolmentStoreProxyStubs.stubAllocateEnrolmentToGroupGroupIdDoesNotExist(groupId, enrolmentKey)
      applicationForRiskingRepo.upsert(readyToSubscribe).futureValue

      subscriptionService.processSubscriptions().futureValue

      persisted(readyToSubscribe).isSubscribed shouldBe false
      persisted(readyToSubscribe).enrolmentFailure shouldBe Some(EnrolmentFailure.GroupDoesNotExist)

    "leaves the application alone when HIP rejects the subscription, so the next run retries it" in:
      AuditStubs.stubAuditWrite()
      HipStubs.stubSubscribeToAgentServicesFailure(readyToSubscribe.applicationData.safeId)
      applicationForRiskingRepo.upsert(readyToSubscribe).futureValue

      subscriptionService.processSubscriptions().futureValue

      EnrolmentStoreProxyStubs.verifyAddKnownFacts(count = 0)
      persisted(readyToSubscribe).isSubscribed shouldBe false
      persisted(readyToSubscribe).enrolmentFailure shouldBe None

    "ignores an application that is already subscribed" in:
      val alreadySubscribed: ApplicationForRisking = TdRiskingInstancesInStates.approvedAfterSubscribed.application
      applicationForRiskingRepo.upsert(alreadySubscribed).futureValue

      subscriptionService.processSubscriptions().futureValue

      HipStubs.verifySubscribeToAgentServices(count = 0)
      EnrolmentStoreProxyStubs.verifyAllocateEnrolmentToGroup(count = 0)
  }
