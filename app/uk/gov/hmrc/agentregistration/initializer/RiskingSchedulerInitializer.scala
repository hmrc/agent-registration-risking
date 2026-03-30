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
import play.api.Logging
import uk.gov.hmrc.agentregistrationrisking.runner.RiskingRunner

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RiskingSchedulerInitializer @Inject() (
  riskingRunner: RiskingRunner,
  scheduler: Scheduler,
  config: Configuration
)
extends Logging:

  initialize()

  private def initialize(): Unit =
    val riskingTask = new RiskingTask(riskingRunner, config)

    if riskingTask.enabled then
      logger.info("Bootstrapping risking scheduler")
      scheduler.schedule(riskingTask)
    else
      logger.info(s"${riskingTask.name} not scheduled as it is not enabled")
