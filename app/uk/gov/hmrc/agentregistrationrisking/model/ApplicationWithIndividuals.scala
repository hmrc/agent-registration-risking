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

package uk.gov.hmrc.agentregistrationrisking.model

import uk.gov.hmrc.agentregistration.shared.ApplicationReference

import java.time.Instant

final case class ApplicationWithIndividuals(
  application: ApplicationForRisking,
  individuals: Seq[IndividualForRisking]
):

  /** Latest moment a Minerva result landed across the entity and all individuals. None if any record is still missing its `riskingCompletedDate`. */
  def latestRiskingCompletedDate: Option[Instant] =
    import cats.implicits._
    for
      appDate <- application.riskingCompletedDate
      individualDates <- individuals.map(_.riskingCompletedDate).toList.sequence
    yield individualDates.foldLeft(appDate)(Ordering[Instant].max)

object ApplicationWithIndividuals:

  def merge(
    applications: Seq[ApplicationForRisking],
    individuals: Seq[IndividualForRisking]
  ): Seq[ApplicationWithIndividuals] =
    val map: Map[ApplicationReference, Seq[IndividualForRisking]] = individuals.groupBy(_.applicationReference)
    applications.map: app =>
      ApplicationWithIndividuals(
        application = app,
        individuals = map.getOrElse(app.applicationReference, Seq.empty)
      )
