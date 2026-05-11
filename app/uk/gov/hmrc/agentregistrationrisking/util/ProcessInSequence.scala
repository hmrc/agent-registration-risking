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

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object ProcessInSequence:

  /** Processes a sequence of items sequentially, applying an asynchronous function to each item in order.
    *
    * This method ensures that the async function `f` is applied to each item one at a time, waiting for each Future to complete before processing the next
    * item. This is useful when order matters or when you need to limit concurrency to avoid overwhelming downstream services.
    */
  def processInSequence[Item, Result](items: Seq[Item])(f: Item => Future[Result])(using ExecutionContext): Future[List[Result]] = items
    .foldLeft[Future[List[Result]]](Future.successful(List.empty[Result])):
      (
        acc: Future[List[Result]],
        item
      ) =>
        acc.flatMap(list => f(item).map(_ :: list))
    .map(_.reverse)

  /** Like [[processInSequence]] but recovers per-item failures by invoking `onError` and returns the count of successful invocations. Use when the caller wants
    * best-effort processing — one item's failure doesn't abort the rest, and the caller decides how to log/record the failure.
    */
  def processInSequenceWithRecovery[A](items: Seq[A])(action: A => Future[Unit])(onError: (A, Throwable) => Unit)(using ExecutionContext): Future[Int] =
    processInSequence(items): item =>
      action(item)
        .map(_ => true)
        .recover:
          case ex =>
            onError(item, ex)
            false
    .map(_.count(identity))
