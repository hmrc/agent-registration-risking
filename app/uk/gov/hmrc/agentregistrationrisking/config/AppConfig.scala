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

package uk.gov.hmrc.agentregistrationrisking.config

import javax.inject.Inject
import javax.inject.Singleton
import play.api.Configuration
import uk.gov.hmrc.agentregistrationrisking.model.sdes.SdesInformationType
import uk.gov.hmrc.agentregistrationrisking.model.sdes.SdesSrn
import uk.gov.hmrc.agentregistrationrisking.model.sdes.SdesServerToken
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.duration.FiniteDuration

@Singleton
class AppConfig @Inject() (
  servicesConfig: ServicesConfig,
  config: Configuration
):

  val appName: String = config.get[String]("appName")
  val hmrcAsAgentEnrolment: Enrolment = Enrolment(key = "HMRC-AS-AGENT")
  val sdesProxyBaseUrl: String = servicesConfig.baseUrl("secure-data-exchange-proxy")
  val sdesInformationType: SdesInformationType = SdesInformationType(config.get[String]("secure-data-exchange-proxy-config.information-type"))
  val sdesServerToken: SdesServerToken = SdesServerToken(config.get[String]("secure-data-exchange-proxy-config.server-token"))
  val sdesSrn: SdesSrn = SdesSrn(config.get[String]("secure-data-exchange-proxy-config.srn"))

  object ApplicationForRiskingRepo:

    val ttl: FiniteDuration = ConfigHelper.readFiniteDuration("mongodb.application-for-risking-ttl", servicesConfig)
