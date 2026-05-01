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

package uk.gov.hmrc.agentregistrationrisking.util

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import uk.gov.hmrc.agentregistrationrisking.testsupport.UnitSpec

@SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures"))
class ProcessInSequenceSpec
extends UnitSpec:

  "processInSequence" - {

    "empty sequence" - {
      "returns an empty list without calling f" in {
        val callCount = mutable.ListBuffer.empty[Int]
        val result =
          ProcessInSequence.processInSequence(Seq.empty[Int]): item =>
            callCount += item
            Future.successful(item)
        result.futureValue shouldBe List.empty
        callCount shouldBe empty
      }
    }

    "single item" - {
      "returns a one-element list with the result of f" in {
        val result = ProcessInSequence.processInSequence(Seq("a"))(item => Future.successful(s"result-$item"))
        result.futureValue shouldBe List("result-a")
      }
    }

    "multiple items" - {
      "preserves input order in the result list" in {
        val result = ProcessInSequence.processInSequence(Seq(1, 2, 3))(item => Future.successful(item * 2))
        result.futureValue shouldBe List(2, 4, 6)
      }

      "processes items in sequence — each item starts only after the previous one finishes" in {
        val callLog = mutable.ListBuffer.empty[String]
        // Varying sleep durations: if items ran concurrently, item 1 (20ms) and item 2 (40ms)
        // would finish before item 0 (80ms), scrambling the log. Strict interleaving proves sequencing.
        val sleepMs = Map(
          0 -> 80,
          1 -> 20,
          2 -> 40
        )

        val resultFuture =
          ProcessInSequence.processInSequence(Seq(0, 1, 2)): item =>
            Future:
              callLog += s"Started $item"
              Thread.sleep(sleepMs(item))
              callLog += s"Finished $item"
              item * 10

        resultFuture.futureValue shouldBe List(0, 10, 20)
        callLog.toList shouldBe List(
          "Started 0",
          "Finished 0",
          "Started 1",
          "Finished 1",
          "Started 2",
          "Finished 2"
        )
      }
    }

    "failure handling" - {
      "propagates a failure from the first item and does not call f for remaining items" in {
        val callLog = mutable.ListBuffer.empty[Int]
        val boom = new RuntimeException("boom")

        val result =
          ProcessInSequence.processInSequence(Seq(0, 1, 2)): item =>
            callLog += item
            if item == 0 then Future.failed(boom) else Future.successful(item)

        result.failed.futureValue shouldBe boom
        callLog.toList shouldBe List(0)
      }

      "propagates a failure from a middle item and does not call f for subsequent items" in {
        val callLog = mutable.ListBuffer.empty[Int]
        val boom = new RuntimeException("middle boom")

        val result =
          ProcessInSequence.processInSequence(Seq(0, 1, 2)): item =>
            callLog += item
            if item == 1 then Future.failed(boom) else Future.successful(item)

        result.failed.futureValue shouldBe boom
        eventually:
          callLog.toList shouldBe List(0, 1)
        callLog should not contain 2
      }

      "propagates a failure from the last item" in {
        val boom = new RuntimeException("last boom")
        val result =
          ProcessInSequence.processInSequence(Seq(0, 1, 2)): item =>
            if item == 2 then Future.failed(boom) else Future.successful(item)

        result.failed.futureValue shouldBe boom
      }
    }
  }
