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

package uk.gov.hmrc.agentregistrationrisking.runner

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.services.ApplicationOutcomeService
import uk.gov.hmrc.agentregistrationrisking.services.BackendNotificationService
import uk.gov.hmrc.agentregistrationrisking.services.EmailServiceForApprovedApplications
import uk.gov.hmrc.agentregistrationrisking.services.EmailServiceForFailedFixable
import uk.gov.hmrc.agentregistrationrisking.services.EmailServiceForFailedNonFixable
import uk.gov.hmrc.agentregistrationrisking.services.RiskingResultsService
import uk.gov.hmrc.agentregistrationrisking.services.SubscriptionService
import uk.gov.hmrc.agentregistrationrisking.util.EmptyRequest
import uk.gov.hmrc.agentregistrationrisking.util.RequestAwareLogging

import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class RiskingResultsFileProcessingRunner @Inject() (
  riskingResultsService: RiskingResultsService,
  applicationOutcomeService: ApplicationOutcomeService,
  subscriptionService: SubscriptionService,
  emailServiceForApprovedApplications: EmailServiceForApprovedApplications,
  emailServiceForFailedNonFixable: EmailServiceForFailedNonFixable,
  emailServiceForFailedFixable: EmailServiceForFailedFixable,
  backendNotificationService: BackendNotificationService
)(using
  appConfig: AppConfig,
  ec: ExecutionContext,
  clock: Clock
)
extends RequestAwareLogging:

  def run(): Future[Unit] =
    given RequestHeader = EmptyRequest.emptyRequestHeader
    (for
      _ <- riskingResultsService.processResultsFiles()
      _ <- applicationOutcomeService.processOverallOutcomes()
      _ <- subscriptionService.processSubscriptions()
      _ <- emailServiceForApprovedApplications.processEmails()
      _ <- emailServiceForFailedNonFixable.processEmails()
      _ <- emailServiceForFailedFixable.processEmails()
      _ <- backendNotificationService.processBackendNotifications()
    yield ()).recover { case ex: Exception => logger.error(s"Error processing risking results file", ex) }
