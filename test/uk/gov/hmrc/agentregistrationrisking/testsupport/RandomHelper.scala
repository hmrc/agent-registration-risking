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

package uk.gov.hmrc.agentregistrationrisking.testsupport

import scala.util.Random

object RandomHelper:

  import java.security.MessageDigest
  import java.nio.ByteBuffer

  private def seedFromString(s: String): Int = {
    val bytes = MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"))
    ByteBuffer.wrap(bytes).getInt
  }

  def makeRandom(seed: String): Random = new scala.util.Random(RandomHelper.seedFromString(seed))
