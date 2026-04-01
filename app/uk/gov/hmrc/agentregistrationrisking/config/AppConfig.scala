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
import uk.gov.hmrc.agentregistrationrisking.model.hip.HipAuthToken
import uk.gov.hmrc.agentregistrationrisking.model.sdes.SdesInformationType
import uk.gov.hmrc.agentregistrationrisking.model.sdes.SdesSrn
import uk.gov.hmrc.agentregistrationrisking.model.sdes.SdesServerToken
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalTime
import scala.concurrent.duration.FiniteDuration

@Singleton
class AppConfig @Inject() (
  servicesConfig: ServicesConfig,
  config: Configuration
):

  val appName: String = config.get[String]("appName")
  val hmrcAsAgentEnrolment: Enrolment = Enrolment(key = "HMRC-AS-AGENT")
  val hipBaseUrl: String = servicesConfig.baseUrl("hip")
  val hipAuthToken: HipAuthToken = HipAuthToken(config.get[String]("microservice.services.hip.authorization-token"))

  object Scheduler:

    val enabled: Boolean = config.getOptional[Boolean]("scheduler.risking.enabled").getOrElse(false)
    val time: LocalTime = LocalTime.parse(config.get[String]("scheduler.risking.time"))

  object ApplicationForRiskingRepo:

    val ttl: FiniteDuration = ConfigHelper.readFiniteDuration("mongodb.application-for-risking-ttl", servicesConfig)

  object SdesProxy:

    val baseUrl: String = servicesConfig.baseUrl("secure-data-exchange-proxy")
    val inboundInformationType: SdesInformationType = SdesInformationType(config.get[String]("secure-data-exchange-proxy-config.inbound.information-type"))
    val outboundInformationType: SdesInformationType = SdesInformationType(config.get[String]("secure-data-exchange-proxy-config.outbound.information-type"))
    val inboundServerToken: SdesServerToken = SdesServerToken(config.get[String]("secure-data-exchange-proxy-config.inbound.server-token"))
    val outboundServerToken: SdesServerToken = SdesServerToken(config.get[String]("secure-data-exchange-proxy-config.outbound.server-token"))
    val srn: SdesSrn = SdesSrn(config.get[String]("secure-data-exchange-proxy-config.srn"))
