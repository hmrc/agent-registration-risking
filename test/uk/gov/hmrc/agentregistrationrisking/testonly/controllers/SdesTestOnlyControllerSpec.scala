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

package uk.gov.hmrc.agentregistrationrisking.testonly.controllers

import org.mongodb.scala.ObservableFuture
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import play.api.libs.json.JsObject
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.testonly.FindRiskingResultsFilesRequest
import uk.gov.hmrc.agentregistration.shared.testonly.RiskingResultsFileName
import uk.gov.hmrc.agentregistration.shared.testonly.FindRiskingResultsFilesResponse
import uk.gov.hmrc.agentregistrationrisking.testonly.model.RiskingResultsFileContent
import uk.gov.hmrc.agentregistrationrisking.testonly.repos.RiskingResultsFileContentsRepo
import uk.gov.hmrc.agentregistrationrisking.testsupport.ControllerSpec

import java.time.Instant

class SdesTestOnlyControllerSpec
extends ControllerSpec:

  override protected def configOverrides: Map[String, Any] = super.configOverrides ++ Map[String, Any]("play.http.router" -> "testOnlyDoNotUseInAppConf.Routes")

  private val findPath: String = "/agent-registration-risking/test-only/find-risking-results-files/by-references"

  s"POST $findPath should give, for each application and individual asked about, the file holding its record" in:
    Seq(
      entityFile,
      combinedFile,
      unrelatedFile
    ).foreach(riskingResultsFileContent => repo.upsert(riskingResultsFileContent).futureValue)

    val response: HttpResponse = find(FindRiskingResultsFilesRequest(
      applicationReferences = List(
        ApplicationReference("XARN0000001"),
        ApplicationReference("XARN0000002"),
        ApplicationReference("XARN0000404")
      ),
      personReferences = List(PersonReference("PERS0000001"))
    ))

    response.status shouldBe Status.OK
    response.json.as[FindRiskingResultsFilesResponse] shouldBe FindRiskingResultsFilesResponse(
      fileNameByApplicationReference = Map(
        ApplicationReference("XARN0000001") -> combinedFile.riskingResultsFileName,
        ApplicationReference("XARN0000002") -> entityFile.riskingResultsFileName
      ),
      fileNameByPersonReference = Map(PersonReference("PERS0000001") -> combinedFile.riskingResultsFileName)
    )

  s"POST $findPath should give the latest file when a record is, wrongly, in more than one" in:
    Seq(entityFile, laterEntityFile).foreach(riskingResultsFileContent => repo.upsert(riskingResultsFileContent).futureValue)

    val response: HttpResponse = find(FindRiskingResultsFilesRequest(
      applicationReferences = List(ApplicationReference("XARN0000002")),
      personReferences = Nil
    ))

    response.status shouldBe Status.OK
    response.json.as[FindRiskingResultsFilesResponse] shouldBe FindRiskingResultsFilesResponse(
      fileNameByApplicationReference = Map(ApplicationReference("XARN0000002") -> laterEntityFile.riskingResultsFileName),
      fileNameByPersonReference = Map.empty
    )

  s"POST $findPath should give nothing when asked about no application or individual" in:
    repo.upsert(entityFile).futureValue

    val response: HttpResponse = find(FindRiskingResultsFilesRequest(applicationReferences = Nil, personReferences = Nil))

    response.status shouldBe Status.OK
    response.json.as[FindRiskingResultsFilesResponse] shouldBe FindRiskingResultsFilesResponse(Map.empty, Map.empty)

  "the risking results file contents should be indexed by the references in their records, also after deleting all files" in:
    indexNames() should contain allOf ("contentApplicationReferenceIndex", "contentPersonReferenceIndex")

    httpClient
      .post(url"$baseUrl/agent-registration-risking/test-only/delete-all-risking-results-files")
      .execute[HttpResponse]
      .futureValue
      .status shouldBe Status.OK

    indexNames() should contain allOf ("contentApplicationReferenceIndex", "contentPersonReferenceIndex")

  override def beforeEach(): Unit =
    super.beforeEach()
    repo.collection.deleteMany(Filters.empty()).toFuture().futureValue
    ()

  private def find(findRiskingResultsFilesRequest: FindRiskingResultsFilesRequest): HttpResponse =
    httpClient
      .post(url"$baseUrl/agent-registration-risking/test-only/find-risking-results-files/by-references")
      .withBody(Json.toJson(findRiskingResultsFilesRequest))
      .execute[HttpResponse]
      .futureValue

  private def indexNames(): Seq[String] = repo
    .collection
    .listIndexes()
    .toFuture()
    .futureValue
    .flatMap(index => index.get("name").map(_.asString().getValue))

  private lazy val repo: RiskingResultsFileContentsRepo = app.injector.instanceOf[RiskingResultsFileContentsRepo]

  private lazy val entityFile: RiskingResultsFileContent = RiskingResultsFileContent(
    riskingResultsFileName = RiskingResultsFileName("test-only-entity-XARN0000002"),
    content = Json.arr(entityRecord("XARN0000002")),
    uploadedAt = Instant.parse("2026-10-15T10:00:00Z")
  )
  private lazy val combinedFile: RiskingResultsFileContent = RiskingResultsFileContent(
    riskingResultsFileName = RiskingResultsFileName("test-only-combined-20261015T110000000Z"),
    content = Json.arr(
      entityRecord("XARN0000001"),
      individualRecord("PERS0000001"),
      individualRecord("PERS0000002")
    ),
    uploadedAt = Instant.parse("2026-10-15T11:00:00Z")
  )
  private lazy val laterEntityFile: RiskingResultsFileContent = RiskingResultsFileContent(
    riskingResultsFileName = RiskingResultsFileName("test-only-entity-XARN0000002-20261015T130000000Z"),
    content = Json.arr(entityRecord("XARN0000002")),
    uploadedAt = Instant.parse("2026-10-15T13:00:00Z")
  )
  private lazy val unrelatedFile: RiskingResultsFileContent = RiskingResultsFileContent(
    riskingResultsFileName = RiskingResultsFileName("test-only-entity-XARN0000009"),
    content = Json.arr(entityRecord("XARN0000009")),
    uploadedAt = Instant.parse("2026-10-15T12:00:00Z")
  )

  private def entityRecord(applicationReference: String): JsObject = Json.obj(
    "recordType" -> "Entity",
    "applicationReference" -> applicationReference,
    "failures" -> Json.arr()
  )

  private def individualRecord(personReference: String): JsObject = Json.obj(
    "recordType" -> "Individual",
    "personReference" -> personReference,
    "failures" -> Json.arr()
  )
