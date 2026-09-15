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

package uk.gov.hmrc.agentregistrationrisking.connectors

import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.agentregistration.shared.GroupId
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector.EnrolmentRequest
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector.KnownFact
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector.KnownFactsRequest
import uk.gov.hmrc.agentregistrationrisking.model.EnrolmentFailure
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.EnrolmentStoreProxyStubs

class EnrolmentStoreProxyConnectorSpec
extends ISpec:

  val enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector = app.injector.instanceOf[EnrolmentStoreProxyConnector]

  private val groupId: GroupId = GroupId("some-group-id")
  private val enrolmentKey: String = "HMRC-AS-AGENT~AgentReferenceNumber~HARN0001234"

  private val knownFactsRequest: KnownFactsRequest = KnownFactsRequest(
    verifiers = Seq(KnownFact(key = "AgencyPostcode", value = "AB1 2CD"))
  )

  private val enrolmentRequest: EnrolmentRequest = EnrolmentRequest(
    userId = "some-user-id",
    `type` = "principal",
    friendlyName = "Some Agency",
    verifiers = Seq(KnownFact(key = "AgencyPostcode", value = "AB1 2CD"))
  )

  "addKnownFacts completes successfully when the enrolment store responds with 204 No Content" in:
    given RequestHeader = FakeRequest()
    EnrolmentStoreProxyStubs.stubAddKnownFacts(enrolmentKey)
    enrolmentStoreProxyConnector.addKnownFacts(enrolmentKey, knownFactsRequest).futureValue shouldBe (())
    EnrolmentStoreProxyStubs.verifyAddKnownFacts()

  "addKnownFacts fails when the enrolment store responds with a non-2xx status" in:
    given RequestHeader = FakeRequest()
    EnrolmentStoreProxyStubs.stubAddKnownFactsFailure(enrolmentKey)
    val exception = enrolmentStoreProxyConnector.addKnownFacts(enrolmentKey, knownFactsRequest).failed.futureValue
    exception shouldBe a[Throwable]
    EnrolmentStoreProxyStubs.verifyAddKnownFacts()

  "allocateEnrolmentToGroup returns None when the enrolment store responds with 201 Created" in:
    given RequestHeader = FakeRequest()
    EnrolmentStoreProxyStubs.stubAllocateEnrolmentToGroup(groupId.value, enrolmentKey)
    enrolmentStoreProxyConnector.allocateEnrolmentToGroup(
      groupId,
      enrolmentKey,
      enrolmentRequest
    ).futureValue shouldBe None
    EnrolmentStoreProxyStubs.verifyAllocateEnrolmentToGroup()

  "allocateEnrolmentToGroup returns Some(InvalidIdentifiers) when the enrolment store responds with 400 and code INVALID_IDENTIFIERS" in:
    given RequestHeader = FakeRequest()
    EnrolmentStoreProxyStubs.stubAllocateEnrolmentToGroupInvalidIdentifiers(groupId.value, enrolmentKey)
    enrolmentStoreProxyConnector.allocateEnrolmentToGroup(
      groupId,
      enrolmentKey,
      enrolmentRequest
    ).futureValue shouldBe Some(EnrolmentFailure.InvalidIdentifiers)
    EnrolmentStoreProxyStubs.verifyAllocateEnrolmentToGroup()

  "allocateEnrolmentToGroup returns Some(GroupDoesNotExist) when the enrolment store responds with 404 and code GROUP_ID_DOES_NOT_EXIST" in:
    given RequestHeader = FakeRequest()
    EnrolmentStoreProxyStubs.stubAllocateEnrolmentToGroupGroupIdDoesNotExist(groupId.value, enrolmentKey)
    enrolmentStoreProxyConnector.allocateEnrolmentToGroup(
      groupId,
      enrolmentKey,
      enrolmentRequest
    ).futureValue shouldBe Some(EnrolmentFailure.GroupDoesNotExist)
    EnrolmentStoreProxyStubs.verifyAllocateEnrolmentToGroup()

  "allocateEnrolmentToGroup returns Some(AlreadyAllocatedToGroup) when the enrolment store responds with 409 and code MULTIPLE_ENROLMENTS_INVALID" in:
    given RequestHeader = FakeRequest()
    EnrolmentStoreProxyStubs.stubAllocateEnrolmentToGroupMultipleEnrolmentsInvalid(groupId.value, enrolmentKey)
    enrolmentStoreProxyConnector.allocateEnrolmentToGroup(
      groupId,
      enrolmentKey,
      enrolmentRequest
    ).futureValue shouldBe Some(EnrolmentFailure.AlreadyAllocatedToGroup)
    EnrolmentStoreProxyStubs.verifyAllocateEnrolmentToGroup()

  "allocateEnrolmentToGroup fails when the enrolment store responds with 400 and an unrecognised code" in:
    given RequestHeader = FakeRequest()
    EnrolmentStoreProxyStubs.stubAllocateEnrolmentToGroupBadRequestUnrecognisedCode(groupId.value, enrolmentKey)
    val exception =
      enrolmentStoreProxyConnector.allocateEnrolmentToGroup(
        groupId,
        enrolmentKey,
        enrolmentRequest
      ).failed.futureValue
    exception shouldBe a[Throwable]
    EnrolmentStoreProxyStubs.verifyAllocateEnrolmentToGroup()

  "allocateEnrolmentToGroup fails when the enrolment store responds with 409 and a malformed body" in:
    given RequestHeader = FakeRequest()
    EnrolmentStoreProxyStubs.stubAllocateEnrolmentToGroupConflictMalformedBody(groupId.value, enrolmentKey)
    val exception =
      enrolmentStoreProxyConnector.allocateEnrolmentToGroup(
        groupId,
        enrolmentKey,
        enrolmentRequest
      ).failed.futureValue
    exception shouldBe a[Throwable]
    EnrolmentStoreProxyStubs.verifyAllocateEnrolmentToGroup()

  "allocateEnrolmentToGroup fails when the enrolment store responds with a non-2xx status" in:
    given RequestHeader = FakeRequest()
    EnrolmentStoreProxyStubs.stubAllocateEnrolmentToGroupFailure(groupId.value, enrolmentKey)
    val exception =
      enrolmentStoreProxyConnector.allocateEnrolmentToGroup(
        groupId,
        enrolmentKey,
        enrolmentRequest
      ).failed.futureValue
    exception shouldBe a[Throwable]
    EnrolmentStoreProxyStubs.verifyAllocateEnrolmentToGroup()
