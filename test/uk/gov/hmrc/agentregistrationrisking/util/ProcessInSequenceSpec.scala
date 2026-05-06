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

  "processInSequence emptySeq  returns an empty list without calling f" in:
    val callCount = mutable.ListBuffer.empty[Int]
    val result =
      ProcessInSequence.processInSequence(Seq.empty[Int]): item =>
        callCount += item
        Future.successful(item)
    result.futureValue shouldBe List.empty
    callCount shouldBe empty

  "processInSequence singleItem  returns a one-element list with the result of f" in:
    val callCount = mutable.ListBuffer.empty[String]
    val result =
      ProcessInSequence.processInSequence(Seq("a")): item =>
        callCount += item
        Future.successful(s"result-$item")
    result.futureValue shouldBe List("result-a")
    callCount.toList shouldBe List("a")

  "processInSequence multipleItems  processes items in sequence — each item starts only after the previous one finishes" in:
    val items: Seq[Int] = (0 until 10).toList
    val callLog = mutable.ListBuffer.empty[String]
    // Alternating sleep durations: odd items sleep longer than even items.
    // If items ran concurrently, odd items would finish after the next even item, scrambling the log.
    // Strict start/finish interleaving proves sequencing.
    val sleepMs: Int => Int = i => if i % 2 == 0 then 20 else 60

    val resultFuture: Future[List[Int]] =
      ProcessInSequence.processInSequence(items): item =>
        Future:
          callLog += s"Started $item"
          Thread.sleep(sleepMs(item))
          callLog += s"Finished $item"
          item * 10

    resultFuture.futureValue shouldBe items.map(_ * 10)
    callLog.toList shouldBe items.flatMap(i => List(s"Started $i", s"Finished $i"))

  "processInSequence failureHandling  propagates a failure from the first item and does not call f for remaining items" in:
    val callLog = mutable.ListBuffer.empty[Int]
    val boom: RuntimeException = new RuntimeException("boom")

    val result: Future[List[Int]] =
      ProcessInSequence.processInSequence(Seq(0, 1, 2)): item =>
        callLog += item
        if item == 0 then Future.failed(boom) else Future.successful(item)

    result.failed.futureValue shouldBe boom
    callLog.toList shouldBe List(0)

  "processInSequence failureHandling propagates a failure from a middle item and does not call f for subsequent items" in:
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

  "processInSequence failureHandling  propagates a failure from the last item" in:
    val callLog = mutable.ListBuffer.empty[Int]
    val boom = new RuntimeException("last boom")
    val result =
      ProcessInSequence.processInSequence(Seq(0, 1, 2)): item =>
        callLog += item
        if item == 2 then Future.failed(boom) else Future.successful(item)

    result.failed.futureValue shouldBe boom
    callLog.toList shouldBe List(0, 1, 2)
