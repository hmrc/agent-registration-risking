/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationrisking.testsupport.testdata

import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.GroupId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.Utr

import java.util.UUID

trait TdBase:

  def dateString: String = "2059-11-25"
  def timeString: String = s"${dateString}T16:33:51.880"
  def localDateTime: LocalDateTime =
    // the frozen time has to be in future otherwise the applications will disappear from mongodb because of expiry index
    LocalDateTime.parse(timeString, DateTimeFormatter.ISO_DATE_TIME)
  def instant: Instant = localDateTime.toInstant(ZoneOffset.UTC)
  def newInstant: Instant = instant.plusSeconds(20) // used when a new application is created from existing one

  // TODO the test data must be deterministic otherwise they can't be used in tests
  def randomId: String = UUID.randomUUID().toString
  def internalUserId: InternalUserId = InternalUserId("internal-user-id-12345")
  def agentApplicationId: AgentApplicationId = AgentApplicationId("agent-application-id-12345")
  def individualProvidedDetailsId: IndividualProvidedDetailsId = IndividualProvidedDetailsId("individual-id-12345")
  def linkId: LinkId = LinkId("link-id-12345")
  def utr: Utr = Utr("1234567890")
  def nino: Nino = Nino("AA0011221A")
  def email: String = "test@example.com"
  def telephoneNumber: String = "01234567890"
  def groupId: GroupId = GroupId("group-id-12345")
  def applicantName: String = "Test Applicant"
  def individualName: String = "Test Individual"
  def agentBusinessName: String = "Test Agent"
  def crn: String = "OC123456"
  def amlsCode: String = "HMRC"
  def amlsRegistrationNumber: String = "XAML00000123456"
  def vrn: String = "123456789"
  def payeRef: String = "123/AB12345"
  def individualDateOfBirth: LocalDate = LocalDate.of(1980, 1, 1)
