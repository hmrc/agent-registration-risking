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

import play.api.libs.functional.syntax.*
import play.api.libs.json.JsMacroImpl
import play.api.libs.json.Json
import play.api.libs.json.OFormat
import play.api.libs.json.Reads
import play.api.libs.json.__
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.util.Errors
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
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

  /** ES3: Query Enrolments allocated to a group https://confluence.tools.tax.service.gov.uk/display/GGWRLS/ES3+-+Query+Enrolments+allocated+to+a+group
    */
  def queryEnrolmentsAllocatedToGroup(
    groupId: GroupId
  )(using RequestHeader): Future[List[EnrolmentStoreProxyConnector.Enrolment]] =
    val url: URL = url"$baseUrl/groups/${groupId.value}/enrolments"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.OK => (response.json \ "enrolments").as[List[EnrolmentStoreProxyConnector.Enrolment]]
          case Status.NO_CONTENT => List[EnrolmentStoreProxyConnector.Enrolment]()
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed query for EnrolmentsAllocatedToGroup for $groupId")

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
  )(using RequestHeader): Future[Unit] =
    val url: URL = url"$baseUrl/groups/${groupId.value}/enrolments/$enrolmentKey"
    httpClient
      .post(url)
      .withBody(Json.toJson(enrolmentRequest))
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.CREATED => ()
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "POST",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to allocate enrolment $enrolmentKey to group $groupId")

  private val baseUrl: String = appConfig.enrolmentStoreProxyBaseUrl + "/enrolment-store-proxy/enrolment-store"

object EnrolmentStoreProxyConnector:

  final case class Enrolment(
    service: String,
    state: String
  )

  object Enrolment:
    given Reads[Enrolment] =
      (
        (__ \ "service").read[String] and
          (__ \ "state").read[String]
      )(Enrolment.apply)

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
