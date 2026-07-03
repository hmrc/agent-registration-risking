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

import play.api.Logging
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class RiskingSchedulerInitializer @Inject() (
  scheduler: Scheduler,
  appConfig: AppConfig
)(using ExecutionContext)
extends Logging:

  initialize()

  private def initialize(): Unit =
    if appConfig.Scheduler.riskingEnabled then
      logger.info("Bootstrapping risking scheduler")
      scheduler.scheduleDailyRiskingFileUpload(
        appConfig.Scheduler.time
      )
    else
      logger.info("risking not scheduled as it is not enabled")

    if appConfig.Scheduler.resultsEnabled then
      logger.info("Bootstrapping results processing scheduler")
      scheduler.scheduleHourlyResultsFileProcessing()
    else
      logger.info("results processing not scheduled as it is not enabled")
