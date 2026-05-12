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

  "processAllInSequence emptySeq  returns 0 and does not call f or onFailure" in:
    val callLog = mutable.ListBuffer.empty[Int]
    val failureLog = mutable.ListBuffer.empty[(Throwable, Int)]
    val result =
      ProcessInSequence.processAllInSequence(Seq.empty[Int])(item => { callLog += item; Future.successful(item) })((ex, item) => failureLog += ((ex, item)))

    result.futureValue shouldBe 0
    callLog shouldBe empty
    failureLog shouldBe empty

  "processAllInSequence allSucceed  returns a count equal to the number of items" in:
    val result = ProcessInSequence.processAllInSequence(Seq(1, 2, 3))(item => Future.successful(item))((_, _) => ())

    result.futureValue shouldBe 3

  "processAllInSequence singleFailure  calls onFailure with the correct exception and item, returns 0" in:
    val failureLog = mutable.ListBuffer.empty[(Throwable, Int)]
    val boom = new RuntimeException("boom")
    val result = ProcessInSequence.processAllInSequence(Seq(42))(_ => Future.failed(boom))((ex, item) => failureLog += ((ex, item)))

    result.futureValue shouldBe 0
    failureLog.toList shouldBe List((boom, 42))

  "processAllInSequence someFailures  continues processing after failures and returns count of successes" in:
    val callLog = mutable.ListBuffer.empty[Int]
    val failureLog = mutable.ListBuffer.empty[Int]
    val boom = new RuntimeException("boom")
    val result =
      ProcessInSequence.processAllInSequence(Seq(0, 1, 2, 3, 4))(item =>
        callLog += item
        if item % 2 == 1 then Future.failed(boom) else Future.successful(item)
      )((_, item) => failureLog += item)

    result.futureValue shouldBe 3
    callLog.toList shouldBe List(0, 1, 2, 3, 4)
    failureLog.toList shouldBe List(1, 3)

  "processAllInSequence allFail  calls onFailure for every item and returns 0" in:
    val failureLog = mutable.ListBuffer.empty[Int]
    val boom = new RuntimeException("boom")
    val result = ProcessInSequence.processAllInSequence(Seq(0, 1, 2))(item => Future.failed(boom))((_, item) => failureLog += item)

    result.futureValue shouldBe 0
    failureLog.toList shouldBe List(0, 1, 2)

  "processAllInSequence sequencing  processes items in order even when some fail" in:
    val callLog = mutable.ListBuffer.empty[String]
    val boom = new RuntimeException("boom")

    ProcessInSequence.processAllInSequence(Seq(0, 1, 2))(item =>
      Future:
        callLog += s"Started $item"
        Thread.sleep(if item == 1 then 60 else 20)
        callLog += s"Finished $item"
        if item == 1 then throw boom else item
    )((_, _) => ()).futureValue

    callLog.toList shouldBe List(
      "Started 0",
      "Finished 0",
      "Started 1",
      "Finished 1",
      "Started 2",
      "Finished 2"
    )
