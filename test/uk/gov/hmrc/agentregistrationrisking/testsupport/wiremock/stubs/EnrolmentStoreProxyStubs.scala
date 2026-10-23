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

package uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs

import com.github.tomakehurst.wiremock.client.WireMock as wm
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.StubMaker

object EnrolmentStoreProxyStubs:

  private val basePath = "/enrolment-store-proxy/enrolment-store"

  def stubAddKnownFacts(enrolmentKey: String): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.PUT,
    urlPattern = wm.urlEqualTo(s"$basePath/enrolments/$enrolmentKey"),
    responseStatus = 204
  )

  def stubAddKnownFactsFailure(enrolmentKey: String): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.PUT,
    urlPattern = wm.urlEqualTo(s"$basePath/enrolments/$enrolmentKey"),
    responseStatus = 500,
    responseBody = Json.stringify(Json.obj("error" -> "Internal Server Error"))
  )

  def stubAllocateEnrolmentToGroup(
    groupId: String,
    enrolmentKey: String
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"$basePath/groups/$groupId/enrolments/$enrolmentKey"),
    responseStatus = 201
  )

  def stubAllocateEnrolmentToGroupFailure(
    groupId: String,
    enrolmentKey: String
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"$basePath/groups/$groupId/enrolments/$enrolmentKey"),
    responseStatus = 500,
    responseBody = Json.stringify(Json.obj("error" -> "Internal Server Error"))
  )

  def stubAllocateEnrolmentToGroupInvalidIdentifiers(
    groupId: String,
    enrolmentKey: String
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"$basePath/groups/$groupId/enrolments/$enrolmentKey"),
    responseStatus = 400,
    responseBody = Json.stringify(Json.obj(
      "code" -> "INVALID_IDENTIFIERS",
      "message" -> "The enrolment identifiers provided were invalid"
    ))
  )

  def stubAllocateEnrolmentToGroupGroupIdDoesNotExist(
    groupId: String,
    enrolmentKey: String
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"$basePath/groups/$groupId/enrolments/$enrolmentKey"),
    responseStatus = 404,
    responseBody = Json.stringify(Json.obj(
      "code" -> "GROUP_ID_DOES_NOT_EXIST",
      "message" -> "Group Id does NOT exist"
    ))
  )

  def stubAllocateEnrolmentToGroupMultipleEnrolmentsInvalid(
    groupId: String,
    enrolmentKey: String
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"$basePath/groups/$groupId/enrolments/$enrolmentKey"),
    responseStatus = 409,
    responseBody = Json.stringify(Json.obj(
      "code" -> "MULTIPLE_ENROLMENTS_INVALID",
      "message" -> "Multiple Enrolments are not valid for this service"
    ))
  )

  def stubAllocateEnrolmentToGroupBadRequestUnrecognisedCode(
    groupId: String,
    enrolmentKey: String
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"$basePath/groups/$groupId/enrolments/$enrolmentKey"),
    responseStatus = 400,
    responseBody = Json.stringify(Json.obj(
      "code" -> "SOME_OTHER_CODE",
      "message" -> "A 400 that is not INVALID_IDENTIFIERS"
    ))
  )

  def stubAllocateEnrolmentToGroupConflictMalformedBody(
    groupId: String,
    enrolmentKey: String
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"$basePath/groups/$groupId/enrolments/$enrolmentKey"),
    responseStatus = 409,
    responseBody = "not-valid-json"
  )

  def stubQueryPrincipalEnrolmentsForUser(
    userId: String,
    arn: String
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlPathEqualTo(s"$basePath/users/$userId/enrolments"),
    queryParams = Map(
      "service" -> wm.equalTo("HMRC-AS-AGENT"),
      "type" -> wm.equalTo("principal")
    ),
    responseStatus = 200,
    responseBody = Json.stringify(Json.obj(
      "startRecord" -> 1,
      "totalRecords" -> 1,
      "enrolments" -> Json.arr(Json.obj(
        "service" -> "HMRC-AS-AGENT",
        "state" -> "Activated",
        "friendlyName" -> "Some Agency",
        "identifiers" -> Json.arr(Json.obj(
          "key" -> "AgentReferenceNumber",
          "value" -> arn
        ))
      ))
    ))
  )

  def stubQueryPrincipalEnrolmentsForUserEmptyList(userId: String): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlPathEqualTo(s"$basePath/users/$userId/enrolments"),
    queryParams = Map(
      "service" -> wm.equalTo("HMRC-AS-AGENT"),
      "type" -> wm.equalTo("principal")
    ),
    responseStatus = 200,
    responseBody = Json.stringify(Json.obj(
      "startRecord" -> 1,
      "totalRecords" -> 0,
      "enrolments" -> Json.arr()
    ))
  )

  def stubQueryPrincipalEnrolmentsForUserNoContent(userId: String): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlPathEqualTo(s"$basePath/users/$userId/enrolments"),
    queryParams = Map(
      "service" -> wm.equalTo("HMRC-AS-AGENT"),
      "type" -> wm.equalTo("principal")
    ),
    responseStatus = 204
  )

  def stubQueryPrincipalEnrolmentsForUserNotFound(userId: String): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlPathEqualTo(s"$basePath/users/$userId/enrolments"),
    queryParams = Map(
      "service" -> wm.equalTo("HMRC-AS-AGENT"),
      "type" -> wm.equalTo("principal")
    ),
    responseStatus = 404,
    responseBody = Json.stringify(Json.obj(
      "code" -> "INVALID_CREDENTIAL_ID",
      "message" -> "Invalid credential ID"
    ))
  )

  def stubQueryPrincipalEnrolmentsForUserBadRequest(userId: String): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlPathEqualTo(s"$basePath/users/$userId/enrolments"),
    queryParams = Map(
      "service" -> wm.equalTo("HMRC-AS-AGENT"),
      "type" -> wm.equalTo("principal")
    ),
    responseStatus = 400,
    responseBody = Json.stringify(Json.obj(
      "code" -> "INVALID_CREDENTIAL_ID",
      "message" -> "Invalid credential ID"
    ))
  )

  def stubQueryPrincipalEnrolmentsForUserUnexpectedClientError(userId: String): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlPathEqualTo(s"$basePath/users/$userId/enrolments"),
    queryParams = Map(
      "service" -> wm.equalTo("HMRC-AS-AGENT"),
      "type" -> wm.equalTo("principal")
    ),
    responseStatus = 409,
    responseBody = Json.stringify(Json.obj(
      "code" -> "SOME_OTHER_CODE",
      "message" -> "A 4xx the specification does not list"
    ))
  )

  def stubQueryPrincipalEnrolmentsForUserFailure(userId: String): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlPathEqualTo(s"$basePath/users/$userId/enrolments"),
    queryParams = Map(
      "service" -> wm.equalTo("HMRC-AS-AGENT"),
      "type" -> wm.equalTo("principal")
    ),
    responseStatus = 500,
    responseBody = Json.stringify(Json.obj("error" -> "Internal Server Error"))
  )

  def stubAssignEnrolmentToUser(
    userId: String,
    enrolmentKey: String
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"$basePath/users/$userId/enrolments/$enrolmentKey"),
    responseStatus = 201
  )

  def stubAssignEnrolmentToUserForbidden(
    userId: String,
    enrolmentKey: String
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"$basePath/users/$userId/enrolments/$enrolmentKey"),
    responseStatus = 403,
    responseBody = Json.stringify(Json.obj(
      "code" -> "INVALID_CREDENTIAL_ID",
      "message" -> "Invalid credential ID"
    ))
  )

  def stubAssignEnrolmentToUserForbiddenUnrecognisedCode(
    userId: String,
    enrolmentKey: String
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"$basePath/users/$userId/enrolments/$enrolmentKey"),
    responseStatus = 403,
    responseBody = Json.stringify(Json.obj(
      "code" -> "SOME_OTHER_CODE",
      "message" -> "A 403 that is not INVALID_CREDENTIAL_ID"
    ))
  )

  def stubAssignEnrolmentToUserUserIdDoesNotExist(
    userId: String,
    enrolmentKey: String
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"$basePath/users/$userId/enrolments/$enrolmentKey"),
    responseStatus = 404,
    responseBody = Json.stringify(Json.obj(
      "code" -> "USER_ID_DOES_NOT_EXIST",
      "message" -> "User ID does not exist"
    ))
  )

  def stubAssignEnrolmentToUserAllocationDoesNotExist(
    userId: String,
    enrolmentKey: String
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"$basePath/users/$userId/enrolments/$enrolmentKey"),
    responseStatus = 404,
    responseBody = Json.stringify(Json.obj(
      "code" -> "ALLOCATION_DOES_NOT_EXIST",
      "message" -> "The enrolment or client allocation does not exist"
    ))
  )

  def stubAssignEnrolmentToUserBadRequest(
    userId: String,
    enrolmentKey: String
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"$basePath/users/$userId/enrolments/$enrolmentKey"),
    responseStatus = 400,
    responseBody = Json.stringify(Json.obj(
      "code" -> "INVALID_CREDENTIAL_ID",
      "message" -> "Invalid credential ID"
    ))
  )

  def stubAssignEnrolmentToUserUnexpectedClientError(
    userId: String,
    enrolmentKey: String
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"$basePath/users/$userId/enrolments/$enrolmentKey"),
    responseStatus = 409,
    responseBody = Json.stringify(Json.obj(
      "code" -> "SOME_OTHER_CODE",
      "message" -> "A 4xx the specification does not list"
    ))
  )

  def stubAssignEnrolmentToUserFailure(
    userId: String,
    enrolmentKey: String
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"$basePath/users/$userId/enrolments/$enrolmentKey"),
    responseStatus = 500,
    responseBody = Json.stringify(Json.obj("error" -> "Internal Server Error"))
  )

  def verifyAddKnownFacts(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.PUT,
    urlPattern = wm.urlMatching(s"$basePath/enrolments/.*"),
    count = count
  )

  def verifyAllocateEnrolmentToGroup(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlMatching(s"$basePath/groups/.*/enrolments/.*"),
    count = count
  )

  def verifyQueryPrincipalEnrolmentsForUser(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlPathMatching(s"$basePath/users/.*/enrolments"),
    count = count
  )

  def verifyAssignEnrolmentToUser(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlMatching(s"$basePath/users/.*/enrolments/.*"),
    count = count
  )
