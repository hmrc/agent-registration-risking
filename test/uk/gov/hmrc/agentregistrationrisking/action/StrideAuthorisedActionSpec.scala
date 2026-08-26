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

package uk.gov.hmrc.agentregistrationrisking.action

import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import play.api.mvc.Results.Unauthorized
import uk.gov.hmrc.agentregistrationrisking.testsupport.ISpec
import uk.gov.hmrc.agentregistrationrisking.testsupport.wiremock.stubs.AuthStubs

import scala.concurrent.Future

class StrideAuthorisedActionSpec
extends ISpec:

  "successfully authorise user if enrolments has the correct stride role" in:
    val strideAuthorisedAction = app.injector.instanceOf[StrideAuthorisedAction]
    val request: Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthorise(
      requestBodyJson = AuthStubs.expectedPrivilegedApplicationRequestBody,
      responseBody = AuthStubs.expectedResponseBodyWithStrideRole
    )
    val result: Result = Ok("AllGood")
    strideAuthorisedAction
      .invokeBlock(request, _ => Future.successful(result))
      .futureValue shouldBe result
    AuthStubs.verifyAuthorise()

  "successfully throw InternalError if enrolments doesn't have the correct stride role" in:
    val strideAuthorisedAction = app.injector.instanceOf[StrideAuthorisedAction]
    val request: Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthorise(
      requestBodyJson = AuthStubs.expectedPrivilegedApplicationRequestBody,
      responseBody = AuthStubs.expectedResponseBodyWithoutCorrectStrideRole
    )
    strideAuthorisedAction
      .invokeBlock(request, _ => fakeResultF)
      .futureValue shouldBe Unauthorized
    AuthStubs.verifyAuthorise()

    def fakeResultF: Future[Result] = fail("this should not be executed if test works fine")
