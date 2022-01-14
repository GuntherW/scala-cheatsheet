package de.codecentric.wittig.scala.streams

import scala.concurrent.duration.DurationInt

import cats.effect.{IO, IOApp}
import cats.syntax.applicative.catsSyntaxApplicativeId
import fs2.io.file.{Files, Path}
import fs2.{text, Stream}
import fs2.concurrent.{SignallingRef, Topic}

object TimesheetService extends IOApp.Simple {

  private val wlBatchSize  = 10
  private val wlBatchCount = 10

  val stream: Stream[IO, Worklog] =
    Stream.unfoldEval(0) { idxBatch =>
      WebService.getWorklogs(idxBatch, wlBatchSize).delayBy(20.millis)
        .map(i => Option.when(idxBatch < wlBatchCount)(i, idxBatch + 1))
    }.debug(wl => s"retrieved Worklogs: ${wl.mkString}")
      .flatMap(Stream.emits)

  val program: Stream[IO, (List[Worklog], Map[WorklogId, IssueDetails])] =
    for {
      topic             <- Stream.eval(Topic[IO, Worklog])
      publisher          = stream.through(topic.publish) ++ Stream.eval(topic.close)
      subscriberWorklogs = topic.subscribe(wlBatchSize * 2)
                             .debug(e => "worklog-" + e.id)
                             .fold(List.empty[Worklog]) { case (ws, w) => w :: ws }
      subscriberIssues   = topic.subscribe(wlBatchSize * 2).mapAccumulate(Set.empty[WorklogId]) {
                             case (set, worklog) =>
                               if (set(worklog.id)) (set, None)
                               else (set + worklog.id, Some(worklog))
                           }
                             .map(_._2)
                             .collect { case Some(w) => w }
                             .mapAsync(2)(WebService.getIssueDetails)
                             .debug(e => "issue-detail-" + e.head._1.value)
                             .fold(Map.empty[WorklogId, IssueDetails])(_ ++ _)
      result            <- subscriberWorklogs.parZip(subscriberIssues).concurrently(publisher)
    } yield result

  def run: IO[Unit] = program.debug(_.toString).compile.drain
}

// Simulating WS-Calls
object WebService {
  def getWorklogs(index: Int, wlBatchSize: Int): IO[List[Worklog]] =
    IO(List.tabulate(wlBatchSize)(i => Worklog(WorklogId(index * 10 + i), s"Name: ${index * 10 + i}")))

  def getIssueDetails(worklog: Worklog): IO[Map[WorklogId, IssueDetails]] =
    IO(Map(worklog.id -> IssueDetails(worklog.name.toUpperCase))).delayBy(11.millis)
}

// Domain Model
case class WorklogId(value: Int)
case class Worklog(id: WorklogId, name: String)
case class IssueDetails(value: String)
