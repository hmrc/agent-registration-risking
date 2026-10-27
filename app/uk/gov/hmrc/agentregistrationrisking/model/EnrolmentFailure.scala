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

package uk.gov.hmrc.agentregistrationrisking.model

import play.api.libs.json.Format
import uk.gov.hmrc.agentregistration.shared.util.JsonFormatsFactory

enum EnrolmentFailure
derives CanEqual:

  /** 400 INVALID_IDENTIFIERS when enrolment already exists on a different group. */
  case InvalidIdentifiers

  /** 404 GROUP_ID_DOES_NOT_EXIST when group was deleted while risking was running. */
  case GroupDoesNotExist

  /** 409 MULTIPLE_ENROLMENTS_INVALID when the group already has the HMRC-AS-AGENT enrolment. Recovered by AlreadyAllocatedToGroupRecoveryService. */
  case AlreadyAllocatedToGroup

  /** ES11 403 INVALID_CREDENTIAL_ID, or ES2 showing another ARN on the credential: the group holds HMRC-AS-AGENT for a different ARN. */
  case GroupEnrolledForDifferentArn

  /** ES11 404 USER_ID_DOES_NOT_EXIST when the applicant credential no longer exists. */
  case CredentialDoesNotExist

  /** ES2 400: the enrolments query for the applicant credential was rejected. */
  case EnrolmentQueryRejected

  /** ES11 400: the assignment was rejected, invalid credential id or enrolment key, or the user is already assigned the enrolment. */
  case EnrolmentAssignmentRejected

  /** ES11 404 ALLOCATION_DOES_NOT_EXIST: our enrolment key is allocated to no group, so either the group holds a different ARN or the allocation was removed.
    */
  case EnrolmentNotAllocated

object EnrolmentFailure:

  given format: Format[EnrolmentFailure] = JsonFormatsFactory.makeEnumFormat[EnrolmentFailure]
