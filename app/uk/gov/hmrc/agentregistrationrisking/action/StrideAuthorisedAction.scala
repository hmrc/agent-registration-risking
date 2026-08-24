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

package uk.gov.hmrc.agentregistrationrisking.action

import com.google.inject.Inject
import com.google.inject.Singleton
import play.api.mvc.*
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationrisking.util.RequestSupport.hc
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class StrideAuthorisedAction @Inject() (
  af: AuthorisedFunctions,
  appConfig: AppConfig,
  cc: MessagesControllerComponents
)
extends ActionBuilder[Request, AnyContent]
with RequestAwareLogging:

  override def invokeBlock[A](
    request: Request[A],
    block: Request[A] => Future[Result]
  ): Future[Result] =
    given r: Request[A] = request

    af.authorised(
      AuthProviders(PrivilegedApplication)
    ).retrieve(
      Retrievals.allEnrolments
    ).apply:
      case allEnrolments if allEnrolments.enrolments.map(_.key).contains(appConfig.StrideAuth.strideRole) => block(request)
      case _ => Future.failed(InternalError(s"User logged in without stride credentials"))

  override def parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser

  private given ExecutionContext = cc.executionContext
  override protected def executionContext: ExecutionContext = cc.executionContext
