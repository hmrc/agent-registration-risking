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

package uk.gov.hmrc.agentregistrationrisking.stubadapter

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.InternalUserId

object StubsPlanetHeaderAdapter:

  private val StubsPlanetHeaderName: String = "X-Planet-Id"

  def withStubsPlanetId(
    rh: RequestHeader,
    internalUserId: InternalUserId
  ): RequestHeader =
    internalUserId.value.split("@").drop(1).headOption match
      case Some(planetId) => rh.withHeaders(rh.headers.add(StubsPlanetHeaderName -> planetId))
      case None           => rh
