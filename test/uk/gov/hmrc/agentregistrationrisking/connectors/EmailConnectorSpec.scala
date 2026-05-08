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
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.TdAll.tdAll.*
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.EmailStubs

class EmailConnectorSpec
extends ISpec:

  val connector: EmailConnector = app.injector.instanceOf[EmailConnector]

  "sendEmail" - {

    "completes successfully when the email service responds with 202 Accepted" in:

      given RequestHeader = FakeRequest()

      EmailStubs.stubSendEmail(testEmailInformation)

      connector.sendEmail(testEmailInformation).futureValue shouldBe (())

      EmailStubs.verifySendEmail()

    "fails when the email service responds with a non-2xx status" in:

      given RequestHeader = FakeRequest()

      EmailStubs.stubSendEmailFailure(testEmailInformation)

      val exception = connector.sendEmail(testEmailInformation).failed.futureValue

      exception shouldBe a[Throwable]
      EmailStubs.verifySendEmail()
  }
