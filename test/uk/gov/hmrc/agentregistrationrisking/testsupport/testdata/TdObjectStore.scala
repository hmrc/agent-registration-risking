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

package uk.gov.hmrc.agentregistrationrisking.testsupport.testdata

import play.api.libs.json.JsObject
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase

trait TdObjectStore { dependencies: TdBase =>

  def fileName: String = "asa_risking_file_version1_0_4_20591125_163351.txt"

  def objectStoreLocation: String = s"agent-registration-risking/applications-for-risking/$fileName"
  def sizeInBytes: Long = 12345L

  def objectStoreUploadResponse: JsObject = Json.obj(
    "location" -> objectStoreLocation,
    "contentLength" -> sizeInBytes,
    "contentMD5" -> "a3c2f1e38701bd2c7b54ebd7b1cd0dbc",
    "lastModified" -> dependencies.nowAsInstant
  )

  def objectStoreUploadFromUrlResponse(uploadedPath: String): JsObject = Json.obj(
    "location" -> uploadedPath,
    "contentLength" -> sizeInBytes,
    "contentMD5" -> "a3c2f1e38701bd2c7b54ebd7b1cd0dbc",
    "lastModified" -> dependencies.nowAsInstant
  )

}
