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

import play.api.Configuration
import uk.gov.hmrc.agentregistrationrisking.model.hip.HipAuthToken
import uk.gov.hmrc.agentregistrationrisking.model.sdes.SdesInformationType
import uk.gov.hmrc.agentregistrationrisking.model.sdes.SdesServerToken
import uk.gov.hmrc.agentregistrationrisking.model.sdes.SdesSrn
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.duration.FiniteDuration

object AppConfig:
  val zoneId: ZoneId = ZoneId.of("UTC")

@Singleton
class AppConfig @Inject() (
  servicesConfig: ServicesConfig,
  config: Configuration
):

  val appName: String = config.get[String]("appName")
  val emailBaseUrl: String = servicesConfig.baseUrl("email")
  val enrolmentStoreProxyBaseUrl: String = servicesConfig.baseUrl("enrolment-store-proxy")
  val hmrcAsAgentEnrolment: Enrolment = Enrolment(key = "HMRC-AS-AGENT")
  val hipBaseUrl: String = servicesConfig.baseUrl("hip")
  val hipAuthToken: HipAuthToken = HipAuthToken(config.get[String]("microservice.services.hip.authorization-token"))

  object AmlsEvidence:
    val baseUrl: String = ConfigHelper.readConfigAsValidUrlString("urls.agent-helpdesk-amls-evidence", config)

  object Scheduler:

    val enabled: Boolean = config.getOptional[Boolean]("scheduler.risking.enabled").getOrElse(false)
    val time: LocalTime = LocalTime.parse(config.get[String]("scheduler.risking.time"))

  object ApplicationForRiskingRepo:
    val ttl: FiniteDuration = ConfigHelper.readFiniteDuration("mongodb.application-for-risking-ttl", servicesConfig)

  object FieldLevelEncryption:

    val enabled: Boolean = config.get[Boolean]("field-level-encryption.enabled")
    val key: String = config.get[String]("field-level-encryption.key")
    val previousKeys: Seq[String] = config.get[Seq[String]]("field-level-encryption.previousKeys")

  object SdesProxy:

    val baseUrl: String = servicesConfig.baseUrl("secure-data-exchange-proxy")
    val inboundInformationType: SdesInformationType = SdesInformationType(
      ConfigHelper.getConfString("secure-data-exchange-proxy.inbound.information-type", servicesConfig)
    )
    val outboundInformationType: SdesInformationType = SdesInformationType(
      ConfigHelper.getConfString("secure-data-exchange-proxy.outbound.information-type", servicesConfig)
    )
    val inboundServerToken: SdesServerToken = SdesServerToken(ConfigHelper.getConfString("secure-data-exchange-proxy.inbound.server-token", servicesConfig))
    val outboundServerToken: SdesServerToken = SdesServerToken(ConfigHelper.getConfString("secure-data-exchange-proxy.outbound.server-token", servicesConfig))
    val srn: SdesSrn = SdesSrn(ConfigHelper.getConfString("secure-data-exchange-proxy.srn", servicesConfig))
    val objectStoreLocationPrefix: String = ConfigHelper.getConfString("secure-data-exchange-proxy.object-store-location-prefix", servicesConfig)

  object Email:
    val applicationProcessingTime: String = config.get[String]("email.application-processing-time")
