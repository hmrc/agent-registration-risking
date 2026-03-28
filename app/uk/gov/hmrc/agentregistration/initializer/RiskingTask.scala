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

package uk.gov.hmrc.agentregistration.initializer

import play.api.Configuration
import play.api.mvc.{Headers, RequestHeader}
import play.api.mvc.request.RequestTarget
import uk.gov.hmrc.agentregistration.initializer.model.{ScheduledTime, Task}
import uk.gov.hmrc.agentregistrationrisking.runner.RiskingRunner

import java.time.LocalTime
import scala.concurrent.Future

class RiskingTask(val riskingRunner: RiskingRunner, config: Configuration) extends Task[Future[Unit]] {

  val name: String   = "risking"
  val repeat: Boolean = true

  override def enabled: Boolean = config.getOptional[Boolean]("scheduler.risking.enabled").getOrElse(false)

  override def scheduledTime: ScheduledTime = {
    val raw = config.get[String]("scheduler.risking.time")
    ScheduledTime(LocalTime.parse(raw))
  }

  override def run(): Future[Unit] = {
    given RequestHeader = syntheticRequestHeader
    riskingRunner.run()
  }

  private val syntheticRequestHeader: RequestHeader = new RequestHeader {
    override def target: RequestTarget  = RequestTarget("/", "/", Map.empty)
    override def version: String        = "HTTP/1.1"
    override def method: String         = "GET"
    override def headers: Headers       = Headers()
    override def connection: play.api.mvc.request.RemoteConnection =
      play.api.mvc.request.RemoteConnection(java.net.InetAddress.getLoopbackAddress, false, None)
    override def attrs: play.libs.typedmap.TypedMap = play.libs.typedmap.TypedMap.empty()
  }
}
