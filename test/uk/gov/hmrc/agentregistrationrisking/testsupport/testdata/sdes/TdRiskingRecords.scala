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

package uk.gov.hmrc.agentregistrationrisking.testsupport.testdata.sdes

import play.api.libs.json.*
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistrationrisking.model.RecordType.*
import uk.gov.hmrc.agentregistrationrisking.model.AdditionalInfo
import uk.gov.hmrc.agentregistrationrisking.model.Failure
import uk.gov.hmrc.agentregistrationrisking.model.RiskingResultRecord
import uk.gov.hmrc.agentregistrationrisking.model.sdes.AvailableFile

trait TdRiskingRecords:

  val testFileName = "testFileName"
  val testDownloadUrl = s"http://localhost:11111/test-location/testFile"
  val testFileSize = 1024

  val testAvailableFile = AvailableFile(
    testDownloadUrl,
    testFileName,
    testFileSize
  )

  val passRecordArrayFileWithNonExistentApp: JsArray = Json.arr(
    Json.obj(
      "recordType" -> Entity,
      "applicationReference" -> "ABC123456",
      "failures" -> Json.arr()
    ),
    Json.obj(
      "recordType" -> Individual,
      "personReference" -> "1234567890",
      "failures" -> Json.arr()
    ),
    Json.obj(
      "recordType" -> Entity,
      "applicationReference" -> "NON_EXISTENT",
      "failures" -> Json.arr()
    )
  )

  val passRecordArrayFile: JsArray = Json.arr(
    Json.obj(
      "recordType" -> Entity,
      "applicationReference" -> "ABC123456",
      "failures" -> Json.arr()
    ),
    Json.obj(
      "recordType" -> Individual,
      "personReference" -> "1234567890",
      "failures" -> Json.arr()
    )
  )

  val failRecordArrayFileMatchingApp: JsArray = Json.arr(
    Json.obj(
      "recordType" -> Entity,
      "applicationReference" -> "ABC123456",
      "failures" -> Json.arr(
        Json.obj(
          "reasonCode" -> "3.2",
          "reasonDescription" -> "AML check failed due to suspicious activity",
          "checkId" -> "3",
          "checkDescription" -> "AMLS"
        )
      )
    ),
    Json.obj(
      "recordType" -> Individual,
      "personReference" -> "1234567890",
      "failures" -> Json.arr(
        Json.obj(
          "reasonCode" -> "4.1",
          "reasonDescription" -> "Outstanding returns overdue",
          "checkId" -> "4",
          "checkDescription" -> "Overdue returns"
        )
      )
    )
  )

  val failRecordArrayFile: JsArray = Json.arr(
    Json.obj(
      "recordType" -> Entity,
      "applicationReference" -> "applicationReference",
      "failures" -> Json.arr(
        Json.obj(
          "reasonCode" -> "3.2",
          "reasonDescription" -> "AML check failed due to suspicious activity",
          "checkId" -> "3",
          "checkDescription" -> "AMLS"
        ),
        Json.obj(
          "reasonCode" -> "4.1",
          "reasonDescription" -> "Outstanding returns overdue",
          "checkId" -> "4",
          "checkDescription" -> "Overdue returns",
          "additionalInfo" -> Json.obj(
            "value" -> 12500.75
          )
        )
      )
    ),
    Json.obj(
      "recordType" -> Individual,
      "personReference" -> "personReference_002",
      "failures" -> Json.arr(
        Json.obj(
          "reasonCode" -> "5.3",
          "reasonDescription" -> "Credit score below acceptable threshold",
          "checkId" -> "5",
          "checkDescription" -> "Credit Risk Assessment"
        )
      )
    ),
    Json.obj(
      "recordType" -> Individual,
      "personReference" -> "personReference_003",
      "failures" -> Json.arr(
        Json.obj(
          "reasonCode" -> "4.2",
          "reasonDescription" -> "Multiple overdue payments detected",
          "checkId" -> "4",
          "checkDescription" -> "Overdue returns",
          "additionalInfo" -> Json.obj(
            "value" -> 3200
          )
        )
      )
    )
  )

  val failRecord1 = RiskingResultRecord(
    "Entity",
    Some(ApplicationReference("applicationReference")),
    Some(List(
      Failure(
        "3.2",
        "AML check failed due to suspicious activity",
        "3",
        "AMLS",
        None
      ),
      Failure(
        "4.1",
        "Outstanding returns overdue",
        "4",
        "Overdue returns",
        Some(AdditionalInfo(12500.75))
      )
    )),
    None
  )

  val failRecord2 = RiskingResultRecord(
    "Individual",
    None,
    Some(List(
      Failure(
        "5.3",
        "Credit score below acceptable threshold",
        "5",
        "Credit Risk Assessment",
        None
      )
    )),
    Some(PersonReference("personReference_002"))
  )

  val failRecord3 = RiskingResultRecord(
    "Individual",
    None,
    Some(List(
      Failure(
        "4.2",
        "Multiple overdue payments detected",
        "4",
        "Overdue returns",
        Some(AdditionalInfo(3200))
      )
    )),
    Some(PersonReference("personReference_003"))
  )

  val passRecord1 = RiskingResultRecord(
    recordType = "Entity",
    applicationReference = Some(ApplicationReference("ABC123456")),
    failures = Some(List.empty),
    personReference = None
  )

  val passRecord2 = RiskingResultRecord(
    recordType = "Individual",
    applicationReference = None,
    failures = Some(List.empty),
    personReference = Some(PersonReference("1234567890"))
  )
