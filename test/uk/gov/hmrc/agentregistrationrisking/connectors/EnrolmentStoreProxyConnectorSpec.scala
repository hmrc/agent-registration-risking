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
import uk.gov.hmrc.agentregistration.shared.Arn
import uk.gov.hmrc.agentregistration.shared.GroupId
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector.AssignEnrolmentResult
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector.AssignedEnrolment
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector.EnrolmentIdentifier
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector.EnrolmentRequest
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector.KnownFact
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector.KnownFactsRequest
import uk.gov.hmrc.agentregistrationrisking.connectors.EnrolmentStoreProxyConnector.QueryEnrolmentsResult
import uk.gov.hmrc.agentregistrationrisking.model.EnrolmentFailure
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.EnrolmentStoreProxyStubs

class EnrolmentStoreProxyConnectorSpec
extends ISpec:

  val enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector = app.injector.instanceOf[EnrolmentStoreProxyConnector]

  private val groupId: GroupId = GroupId("some-group-id")
  private val userId: String = "some-user-id"
  private val arn: Arn = tdAll.arn
  private val serviceName: String = "HMRC-AS-AGENT"
  private val enrolmentKey: String = s"$serviceName~AgentReferenceNumber~${arn.value}"

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

  "queryPrincipalEnrolmentsForUser returns the enrolments when the enrolment store responds with 200" in:
    given RequestHeader = FakeRequest()
    EnrolmentStoreProxyStubs.stubQueryPrincipalEnrolmentsForUser(userId, arn.value)
    enrolmentStoreProxyConnector.queryPrincipalEnrolmentsForUser(userId, serviceName).futureValue shouldBe QueryEnrolmentsResult.Enrolments(
      Seq(AssignedEnrolment(service = serviceName, identifiers = Seq(EnrolmentIdentifier(key = "AgentReferenceNumber", value = arn.value))))
    )
    EnrolmentStoreProxyStubs.verifyQueryPrincipalEnrolmentsForUser()

  "queryPrincipalEnrolmentsForUser returns no enrolments when the enrolment store responds with 200 and an empty list" in:
    given RequestHeader = FakeRequest()
    EnrolmentStoreProxyStubs.stubQueryPrincipalEnrolmentsForUserEmptyList(userId)
    enrolmentStoreProxyConnector.queryPrincipalEnrolmentsForUser(userId, serviceName).futureValue shouldBe QueryEnrolmentsResult.Enrolments(Seq.empty)
    EnrolmentStoreProxyStubs.verifyQueryPrincipalEnrolmentsForUser()

  "queryPrincipalEnrolmentsForUser returns no enrolments when the enrolment store responds with 204 No Content" in:
    given RequestHeader = FakeRequest()
    EnrolmentStoreProxyStubs.stubQueryPrincipalEnrolmentsForUserNoContent(userId)
    enrolmentStoreProxyConnector.queryPrincipalEnrolmentsForUser(userId, serviceName).futureValue shouldBe QueryEnrolmentsResult.Enrolments(Seq.empty)
    EnrolmentStoreProxyStubs.verifyQueryPrincipalEnrolmentsForUser()

  "queryPrincipalEnrolmentsForUser returns CredentialNotFound when the enrolment store responds with 404 INVALID_CREDENTIAL_ID" in:
    given RequestHeader = FakeRequest()
    EnrolmentStoreProxyStubs.stubQueryPrincipalEnrolmentsForUserNotFound(userId)
    enrolmentStoreProxyConnector.queryPrincipalEnrolmentsForUser(userId, serviceName).futureValue shouldBe QueryEnrolmentsResult.CredentialNotFound
    EnrolmentStoreProxyStubs.verifyQueryPrincipalEnrolmentsForUser()

  "queryPrincipalEnrolmentsForUser returns QueryRejected when the enrolment store responds with 400" in:
    given RequestHeader = FakeRequest()
    EnrolmentStoreProxyStubs.stubQueryPrincipalEnrolmentsForUserBadRequest(userId)
    enrolmentStoreProxyConnector.queryPrincipalEnrolmentsForUser(userId, serviceName).futureValue shouldBe QueryEnrolmentsResult.QueryRejected
    EnrolmentStoreProxyStubs.verifyQueryPrincipalEnrolmentsForUser()

  "queryPrincipalEnrolmentsForUser returns QueryRejected when the enrolment store responds with a 4xx the specification does not list" in:
    given RequestHeader = FakeRequest()
    EnrolmentStoreProxyStubs.stubQueryPrincipalEnrolmentsForUserUnexpectedClientError(userId)
    enrolmentStoreProxyConnector.queryPrincipalEnrolmentsForUser(userId, serviceName).futureValue shouldBe QueryEnrolmentsResult.QueryRejected
    EnrolmentStoreProxyStubs.verifyQueryPrincipalEnrolmentsForUser()

  "queryPrincipalEnrolmentsForUser fails when the enrolment store responds with a 5xx status" in:
    given RequestHeader = FakeRequest()
    EnrolmentStoreProxyStubs.stubQueryPrincipalEnrolmentsForUserFailure(userId)
    val exception: Throwable = enrolmentStoreProxyConnector.queryPrincipalEnrolmentsForUser(userId, serviceName).failed.futureValue
    exception shouldBe a[Throwable]
    EnrolmentStoreProxyStubs.verifyQueryPrincipalEnrolmentsForUser()

  "assignEnrolmentToUser returns Assigned when the enrolment store responds with 201 Created" in:
    given RequestHeader = FakeRequest()
    EnrolmentStoreProxyStubs.stubAssignEnrolmentToUser(userId, enrolmentKey)
    enrolmentStoreProxyConnector.assignEnrolmentToUser(userId, enrolmentKey).futureValue shouldBe AssignEnrolmentResult.Assigned
    EnrolmentStoreProxyStubs.verifyAssignEnrolmentToUser()

  "assignEnrolmentToUser returns NotAllocatedToGroup when the enrolment store responds with 403 INVALID_CREDENTIAL_ID" in:
    given RequestHeader = FakeRequest()
    EnrolmentStoreProxyStubs.stubAssignEnrolmentToUserForbidden(userId, enrolmentKey)
    enrolmentStoreProxyConnector.assignEnrolmentToUser(userId, enrolmentKey).futureValue shouldBe AssignEnrolmentResult.NotAllocatedToGroup
    EnrolmentStoreProxyStubs.verifyAssignEnrolmentToUser()

  "assignEnrolmentToUser returns AssignmentRejected when the enrolment store responds with 403 and an unrecognised code" in:
    given RequestHeader = FakeRequest()
    EnrolmentStoreProxyStubs.stubAssignEnrolmentToUserForbiddenUnrecognisedCode(userId, enrolmentKey)
    enrolmentStoreProxyConnector.assignEnrolmentToUser(userId, enrolmentKey).futureValue shouldBe AssignEnrolmentResult.AssignmentRejected
    EnrolmentStoreProxyStubs.verifyAssignEnrolmentToUser()

  "assignEnrolmentToUser returns UserDoesNotExist when the enrolment store responds with 404 USER_ID_DOES_NOT_EXIST" in:
    given RequestHeader = FakeRequest()
    EnrolmentStoreProxyStubs.stubAssignEnrolmentToUserUserIdDoesNotExist(userId, enrolmentKey)
    enrolmentStoreProxyConnector.assignEnrolmentToUser(userId, enrolmentKey).futureValue shouldBe AssignEnrolmentResult.UserDoesNotExist
    EnrolmentStoreProxyStubs.verifyAssignEnrolmentToUser()

  "assignEnrolmentToUser returns AllocationDoesNotExist when the enrolment store responds with 404 ALLOCATION_DOES_NOT_EXIST" in:
    given RequestHeader = FakeRequest()
    EnrolmentStoreProxyStubs.stubAssignEnrolmentToUserAllocationDoesNotExist(userId, enrolmentKey)
    enrolmentStoreProxyConnector.assignEnrolmentToUser(userId, enrolmentKey).futureValue shouldBe AssignEnrolmentResult.AllocationDoesNotExist
    EnrolmentStoreProxyStubs.verifyAssignEnrolmentToUser()

  "assignEnrolmentToUser returns AssignmentRejected when the enrolment store responds with 400" in:
    given RequestHeader = FakeRequest()
    EnrolmentStoreProxyStubs.stubAssignEnrolmentToUserBadRequest(userId, enrolmentKey)
    enrolmentStoreProxyConnector.assignEnrolmentToUser(userId, enrolmentKey).futureValue shouldBe AssignEnrolmentResult.AssignmentRejected
    EnrolmentStoreProxyStubs.verifyAssignEnrolmentToUser()

  "assignEnrolmentToUser returns AssignmentRejected when the enrolment store responds with a 4xx the specification does not list" in:
    given RequestHeader = FakeRequest()
    EnrolmentStoreProxyStubs.stubAssignEnrolmentToUserUnexpectedClientError(userId, enrolmentKey)
    enrolmentStoreProxyConnector.assignEnrolmentToUser(userId, enrolmentKey).futureValue shouldBe AssignEnrolmentResult.AssignmentRejected
    EnrolmentStoreProxyStubs.verifyAssignEnrolmentToUser()

  "assignEnrolmentToUser fails when the enrolment store responds with a 5xx status" in:
    given RequestHeader = FakeRequest()
    EnrolmentStoreProxyStubs.stubAssignEnrolmentToUserFailure(userId, enrolmentKey)
    val exception: Throwable = enrolmentStoreProxyConnector.assignEnrolmentToUser(userId, enrolmentKey).failed.futureValue
    exception shouldBe a[Throwable]
    EnrolmentStoreProxyStubs.verifyAssignEnrolmentToUser()
