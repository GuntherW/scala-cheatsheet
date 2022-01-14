package de.codecentric.wittig.scala.streams

import cats.effect.{IO, IOApp}
import cats.syntax.applicative.catsSyntaxApplicativeId
import fs2.{text, Stream}
import fs2.io.file.{Files, Path}

import cats.effect.{IO, IOApp}
import fs2.Stream
import fs2.concurrent.{SignallingRef, Topic}

import scala.concurrent.duration.DurationInt

object TimesheetService extends IOApp.Simple {

  case class WorklogId(value: Int)
  case class Worklog(id: WorklogId, name: String)
  case class IssueDetails(value: String)

  private def importIssueDetails(worklog: Worklog): IO[Map[WorklogId, IssueDetails]] = IO(
    Map(worklog.id -> IssueDetails(worklog.name.toUpperCase))
  )

  val stream: Stream[IO, Worklog] =
    Stream.unfoldEval(0) { i =>
      IO.sleep(5.millis) *> IO(Option.when(i < 10)(Worklog(WorklogId(i), s"Name: $i"), i + 1))
    }.debug(_.toString)

  val program: Stream[IO, (List[Worklog], Map[WorklogId, IssueDetails])] =
    for {
      topic             <- Stream.eval(Topic[IO, Worklog])
      publisher          = stream.through(topic.publish) ++ Stream.eval(topic.close)
      subscriberWorklogs = topic.subscribe(5)
                             .debug(e => "worklog-" + e.id)
                             .fold(List.empty[Worklog]) { case (ws, w) => w :: ws }
      subscriberIssues   = topic.subscribe(5).mapAccumulate(Set.empty[WorklogId]) {
                             case (set, worklog) =>
                               if (set(worklog.id)) (set, None)
                               else (set + worklog.id, Some(worklog))
                           }
                             .map(_._2)
                             .collect { case Some(w) => w }
                             .mapAsync(5)(importIssueDetails)
                             .debug(e => "issue-detail-" + e.head._1.value)
                             .fold(Map.empty[WorklogId, IssueDetails])(_ ++ _)
      result            <- subscriberWorklogs.parZip(subscriberIssues).concurrently(publisher)
    } yield result

  def run: IO[Unit] = program.debug(_.toString).compile.drain
}
