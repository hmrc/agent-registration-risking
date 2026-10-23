/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.json.Json
import play.api.libs.json.OFormat
import play.api.libs.json.Reads
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.util.Errors
import uk.gov.hmrc.agentregistration.shared.util.HttpErrorBody.errorBody
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.model.EnrolmentFailure
import uk.gov.hmrc.agentregistrationrisking.util.FutureUtil.andLogOnFailure
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class EnrolmentStoreProxyConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig
)(using
  ExecutionContext
)
extends Connector:

  /** ES6: Add KnownFacts for an enrolment https://confluence.tools.tax.service.gov.uk/display/GGWRLS/ES6+-+Upsert+a+known+fact+record
    */
  def addKnownFacts(
    enrolmentKey: String,
    knownFactsRequest: EnrolmentStoreProxyConnector.KnownFactsRequest
  )(using RequestHeader): Future[Unit] =
    val url: URL = url"$baseUrl/enrolments/$enrolmentKey"
    httpClient
      .put(url)
      .withBody(Json.toJson(knownFactsRequest))
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.NO_CONTENT => ()
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "PUT",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to add KnownFacts for enrolment $enrolmentKey")

  /** ES8: Allocate an enrolment to a group https://confluence.tools.tax.service.gov.uk/display/GGWRLS/ES8+-+Allocate+an+enrolment+to+a+group */
  def allocateEnrolmentToGroup(
    groupId: GroupId,
    enrolmentKey: String,
    enrolmentRequest: EnrolmentStoreProxyConnector.EnrolmentRequest
  )(using RequestHeader): Future[Option[EnrolmentFailure]] =
    val url: URL = url"$baseUrl/groups/${groupId.value}/enrolments/$enrolmentKey"
    httpClient
      .post(url)
      .withBody(Json.toJson(enrolmentRequest))
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.CREATED => None
          case Status.BAD_REQUEST if response.errorBody.exists(_.code === "INVALID_IDENTIFIERS") => Some(EnrolmentFailure.InvalidIdentifiers)
          case Status.NOT_FOUND if response.errorBody.exists(_.code === "GROUP_ID_DOES_NOT_EXIST") => Some(EnrolmentFailure.GroupDoesNotExist)
          case Status.CONFLICT if response.errorBody.exists(_.code === "MULTIPLE_ENROLMENTS_INVALID") => Some(EnrolmentFailure.AlreadyAllocatedToGroup)
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "POST",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to allocate enrolment $enrolmentKey to group $groupId")

  /** ES2: Query enrolments assigned to a user https://confluence.tools.tax.service.gov.uk/display/GGWRLS/ES2+-+Query+Enrolments+assigned+to+a+user */
  def queryPrincipalEnrolmentsForUser(
    userId: String,
    service: String
  )(using RequestHeader): Future[EnrolmentStoreProxyConnector.QueryEnrolmentsResult] =
    val url: URL = url"$baseUrl/users/$userId/enrolments?service=$service&type=principal"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.OK =>
            EnrolmentStoreProxyConnector.QueryEnrolmentsResult.Enrolments(
              (response.json \ "enrolments").as[Seq[EnrolmentStoreProxyConnector.AssignedEnrolment]]
            )
          case Status.NO_CONTENT => EnrolmentStoreProxyConnector.QueryEnrolmentsResult.Enrolments(Seq.empty)
          case Status.NOT_FOUND if response.errorBody.exists(_.code === "INVALID_CREDENTIAL_ID") =>
            EnrolmentStoreProxyConnector.QueryEnrolmentsResult.CredentialNotFound
          case status if Status.isClientError(status) =>
            logger.warn(s"ES2 rejected the enrolments query for user $userId with $status: ${response.body}")
            EnrolmentStoreProxyConnector.QueryEnrolmentsResult.QueryRejected
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to query principal enrolments for user $userId and service $service")

  /** ES11: Assign an enrolment to a user https://confluence.tools.tax.service.gov.uk/display/GGWRLS/ES11+-+Assign+an+enrolment+to+a+user */
  def assignEnrolmentToUser(
    userId: String,
    enrolmentKey: String
  )(using RequestHeader): Future[EnrolmentStoreProxyConnector.AssignEnrolmentResult] =
    val url: URL = url"$baseUrl/users/$userId/enrolments/$enrolmentKey"
    httpClient
      .post(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.CREATED => EnrolmentStoreProxyConnector.AssignEnrolmentResult.Assigned
          case Status.FORBIDDEN if response.errorBody.exists(_.code === "INVALID_CREDENTIAL_ID") =>
            EnrolmentStoreProxyConnector.AssignEnrolmentResult.NotAllocatedToGroup
          case Status.NOT_FOUND if response.errorBody.exists(_.code === "USER_ID_DOES_NOT_EXIST") =>
            EnrolmentStoreProxyConnector.AssignEnrolmentResult.UserDoesNotExist
          case Status.NOT_FOUND if response.errorBody.exists(_.code === "ALLOCATION_DOES_NOT_EXIST") =>
            EnrolmentStoreProxyConnector.AssignEnrolmentResult.AllocationDoesNotExist
          case status if Status.isClientError(status) =>
            logger.warn(s"ES11 rejected assigning $enrolmentKey to user $userId with $status: ${response.body}")
            EnrolmentStoreProxyConnector.AssignEnrolmentResult.AssignmentRejected
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "POST",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to assign enrolment $enrolmentKey to user $userId")

  private val baseUrl: String = appConfig.enrolmentStoreProxyBaseUrl + "/enrolment-store-proxy/enrolment-store"

object EnrolmentStoreProxyConnector:

  final case class KnownFactsRequest(
    verifiers: Seq[KnownFact]
  )

  object KnownFactsRequest:
    given OFormat[KnownFactsRequest] = Json.format[KnownFactsRequest]

  final case class KnownFact(
    key: String,
    value: String
  )

  object KnownFact:
    given OFormat[KnownFact] = Json.format[KnownFact]

  final case class EnrolmentRequest(
    userId: String,
    `type`: String,
    friendlyName: String,
    verifiers: Seq[KnownFact]
  )

  object EnrolmentRequest:
    given OFormat[EnrolmentRequest] = Json.format[EnrolmentRequest]

  final case class AssignedEnrolment(
    service: String,
    identifiers: Seq[EnrolmentIdentifier]
  )

  object AssignedEnrolment:
    given Reads[AssignedEnrolment] = Json.reads[AssignedEnrolment]

  final case class EnrolmentIdentifier(
    key: String,
    value: String
  )

  object EnrolmentIdentifier:
    given Reads[EnrolmentIdentifier] = Json.reads[EnrolmentIdentifier]

  enum QueryEnrolmentsResult
  derives CanEqual:

    case Enrolments(assignedEnrolments: Seq[AssignedEnrolment])

    /** 404 INVALID_CREDENTIAL_ID */
    case CredentialNotFound

    /** Any other 4xx, typically 400: the stored credential id or our query parameters are not valid */
    case QueryRejected

  enum AssignEnrolmentResult
  derives CanEqual:

    case Assigned

    /** 403 INVALID_CREDENTIAL_ID: the enrolment is not allocated to the group of the user */
    case NotAllocatedToGroup

    /** 404 USER_ID_DOES_NOT_EXIST */
    case UserDoesNotExist

    /** 404 ALLOCATION_DOES_NOT_EXIST: our enrolment key is allocated to no group */
    case AllocationDoesNotExist

    /** Any other 4xx, typically 400: invalid credential id or enrolment key, or the user is already assigned the enrolment */
    case AssignmentRejected
