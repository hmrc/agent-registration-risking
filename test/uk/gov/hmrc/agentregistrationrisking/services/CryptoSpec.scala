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

import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec

class CryptoSpec
extends ISpec:

  "crypto should be able to encrypt and decrypt" in:
    val crypto: Crypto = app.injector.instanceOf[Crypto]
    val originalText =
      """
        |00|ARR|SAS|20260308|181542
        |01|Entity|N|c9aaa415-6770-4bff-ac30-5f207977c2b3|Test Applicant|01234567890|test@example.com|LimitedLiabilityPartnership|1234567890|OC123456|123456789,123456789|123/AB12345,123/AB12345|HMRC|XAML00000123456|25-11-2059|evidence-reference-123|||||||||||
        |01|Individual|N||||||||123456789,123456789|123/AB12345,123/AB12345|||||a7f00b81-56e0-44e0-824e-2c54bcd305a1|||Test Individual|01-01-1980|AA0011221A|1234567890|01234567890|test@example.com|Y|Y
        |01|Individual|N||||||||123456789,123456789|123/AB12345,123/AB12345|||||a7f00b81-56e0-44e0-824e-2c54bcd305a1|||Test Individual|01-01-1980|AA0011221A|1234567890|01234567890|test@example.com|Y|Y
        |01|Individual|N||||||||123456789,123456789|123/AB12345,123/AB12345|||||a7f00b81-56e0-44e0-824e-2c54bcd305a1|||Test Individual|01-01-1980|AA0011221A|1234567890|01234567890|test@example.com|Y|Y
        |99|4
        |""".stripMargin
    val encryptedText = crypto.encrypt(originalText)
    encryptedText should not be originalText
    crypto.decrypt(encryptedText) shouldBe originalText
