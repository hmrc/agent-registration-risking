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

package uk.gov.hmrc.agentregistrationrisking.testsupport.testdata

import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.PayeRef
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistration.shared.Vrn
import uk.gov.hmrc.agentregistration.shared.individual.IndividualDateOfBirth
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.IndividualData
import uk.gov.hmrc.agentregistrationrisking.testsupport.RandomHelper

import java.time.LocalDate
import scala.util.Random

object TdIndividualData:

  def make(
    applicationReference: ApplicationReference,
    personReference: PersonReference,
    seed: String
  ): IndividualData =
    val random: Random = RandomHelper.makeRandom(seed)
    IndividualData(
      personReference = personReference,
      individualName = IndividualName(s"IndividualName_$seed"),
      isPersonOfControl = random.nextBoolean(),
      internalUserId = InternalUserId(s"InternalUserId_$seed"),
      individualDateOfBirth = IndividualDateOfBirth.Provided(LocalDate.of(
        1980 + random.nextInt(40),
        1,
        1
      )),
      telephoneNumber = TelephoneNumber(s"01234567-${random.nextInt(1000)}"),
      emailAddress = EmailAddress(s"individual_email_$seed@test.com"),
      individualNino = IndividualNino.Provided(Nino(s"AB123456C_$seed")),
      individualSaUtr = IndividualSaUtr.Provided(SaUtr(s"1234567895_$seed")),
      vrns = List(Vrn(s"vrn_$seed")),
      payeRefs = List(PayeRef(s"payeref_$seed")),
      passedIv = random.nextBoolean()
    )
