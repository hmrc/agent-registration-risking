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

package uk.gov.hmrc.agentregistrationrisking.scheduler

import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.runner.RiskingRunner
import uk.gov.hmrc.agentregistrationrisking.scheduler.model.ScheduledTime
import uk.gov.hmrc.agentregistrationrisking.scheduler.model.Task

import scala.concurrent.Future

class RiskingTask(
  val riskingRunner: RiskingRunner,
  appConfig: AppConfig
)
extends Task[Future[Unit]]:

  val name: String = "risking"
  val repeat: Boolean = true

  override def enabled: Boolean = appConfig.Scheduler.enabled

  override def scheduledTime: ScheduledTime = ScheduledTime(appConfig.Scheduler.time)

  override def run(): Future[Unit] = riskingRunner.run()
