/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationrisking.repository

import org.mongodb.scala.model.Aggregates
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes
import org.mongodb.scala.model.Updates
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import ApplicationForRiskingRepoHelp.given
import org.mongodb.scala.result.UpdateResult
import com.mongodb.client.model.Field
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationrisking.config.AppConfig
import uk.gov.hmrc.agentregistrationrisking.model.ApplicationForRisking
import uk.gov.hmrc.agentregistrationrisking.model.IndividualForRisking
import uk.gov.hmrc.agentregistrationrisking.model.RiskingFileName
import uk.gov.hmrc.agentregistrationrisking.repository.Repo.IdExtractor
import uk.gov.hmrc.agentregistrationrisking.repository.Repo.IdString

import java.time.Clock
import java.time.Instant

@Singleton
final class ApplicationForRiskingRepo @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig,
  clock: Clock
)(using ec: ExecutionContext)
extends Repo[ApplicationReference, ApplicationForRisking](
  collectionName = "application-for-risking",
  mongoComponent = mongoComponent,
  indexes = ApplicationForRiskingRepoHelp.indexes(appConfig.ApplicationForRiskingRepo.ttl),
  extraCodecs = Seq(Codecs.playFormatCodec(ApplicationForRisking.format)),
  replaceIndexes = true
):

  def findReadyForSubmission(): Future[Seq[ApplicationForRisking]] = collection
    .find(Filters.exists(FieldNames.riskingFileName, false)) // ready for submissions don't have set riskingFileId
    .toFuture()

  // TODO needs testing?
  def updateRiskingFileId(
    applicationReferences: Seq[ApplicationReference],
    riskingFileName: RiskingFileName
  ): Future[UpdateResult] = collection
    .updateMany(
      Filters.in(FieldNames.applicationReference, applicationReferences.map(_.value)*),
      Updates.combine(
        Updates.set(FieldNames.riskingFileName, riskingFileName.value),
        Updates.set(FieldNames.lastUpdatedAt, Instant.now(clock).toString)
      )
    ).toFuture()

//  def findReadyForSubscription(): Future[Seq[ApplicationForRisking]] = collection
//    .find(
//      Filters.and(
//        Filters.size("failures", 0),
//        Filters.eq("isSubscribed", false)
//      )
//    ).toFuture()

  def findApplicationsPendingAction(): Future[Seq[ApplicationForRisking]] = collection
    .find(
      Filters.and(
        Filters.exists(FieldNames.failures),
        Filters.eq(FieldNames.isSubscribed, false),
        Filters.eq("isEmailSent", false)
      )
    ).toFuture()

//  def findReadyForSubscription(): Future[Seq[ApplicationForRisking]] = collection
//    .find(
//      Filters.and(
//        Filters.size("failures", 0),
//        Filters.eq("isSubscribed", false)
//      )
//    ).toFuture()

  def findNotSubscribedWithResults(): Future[Seq[ApplicationForRisking]] = {
    collection
      .find(
        Filters.and(
          Filters.exists(FieldNames.failures),
          Filters.eq(FieldNames.isSubscribed, false)
        )
      ).toFuture()
  }
  def findSubscribedReadyForSuccessEmail(): Future[Seq[ApplicationForRisking]] = collection
    .find(
      Filters.and(
        Filters.eq("isSubscribed", true),
        Filters.eq("isEmailSent", false)
      )
    ).toFuture()

  def updateEmailSent(applicationReference: ApplicationReference): Future[UpdateResult] = collection
    .updateOne(
      Filters.eq(FieldNames.applicationReference, applicationReference.value),
      Updates.combine(
        Updates.set("isEmailSent", true),
        Updates.set(FieldNames.lastUpdatedAt, Instant.now(clock).toString)
      )
    ).toFuture()

// when named ApplicationForRiskingRepo, Scala 3 compiler complains
// about cyclic reference error during compilation ...
object ApplicationForRiskingRepoHelp:

  given IdString[ApplicationReference] =
    new IdString[ApplicationReference]:
      override def idString(id: ApplicationReference): String = id.value
      override def idField: String = FieldNames.applicationReference

  given IdExtractor[ApplicationForRisking, ApplicationReference] =
    new IdExtractor[ApplicationForRisking, ApplicationReference]:
      override def id(applicationForRisking: ApplicationForRisking): ApplicationReference = applicationForRisking.applicationReference

  def indexes(cacheTtl: FiniteDuration): Seq[IndexModel] = Seq(
    IndexModel(
      keys = Indexes.ascending(FieldNames.applicationReference),
      indexOptions = IndexOptions()
        .name(FieldNames.applicationReferenceIndex)
        .unique(true)
    ),
    IndexModel(
      keys = Indexes.ascending(FieldNames.riskingFileName),
      indexOptions = IndexOptions()
        .name(FieldNames.riskingFileNameIndex)
        .unique(false)
    ),
    IndexModel(
      keys = Indexes.ascending(FieldNames.lastUpdatedAt),
      indexOptions = IndexOptions()
        .expireAfter(cacheTtl.toSeconds, TimeUnit.SECONDS)
        .name(FieldNames.lastUpdatedAtIndex)
    )
  )
