package de.codecentric.wittig.scala.streams

import scala.concurrent.duration.DurationInt

import cats.effect.{IO, IOApp}
import cats.syntax.applicative.catsSyntaxApplicativeId
import fs2.io.file.{Files, Path}
import fs2.{text, Stream}
import fs2.concurrent.{SignallingRef, Topic}

object TimesheetService2 extends IOApp.Simple {

  case class WorklogId(value: Int)
  case class Worklog(id: WorklogId, name: String)
  case class IssueDetails(value: String)

  private def importIssueDetails(worklog: Worklog): IO[Map[WorklogId, IssueDetails]] = IO(
    Map(worklog.id -> IssueDetails(worklog.name.toUpperCase))
  )

  val stream: Stream[IO, Worklog] =
    Stream.unfoldEval(0) { i =>
      IO.sleep(10.millis) *> IO(Option.when(i < 10)(Worklog(WorklogId(i), s"Name: $i"), i + 1))
    }.debug(_.toString)

  val program: Stream[IO, (List[Worklog], Map[WorklogId, IssueDetails])] =
    for
      topic             <- Stream.eval(Topic[IO, Worklog])
      publisher          = stream.through(topic.publish) ++ Stream.eval(topic.close)
      subscriberWorklogs = topic.subscribe(5)
                             .debug(_ => "worklog")
                             .map(Left(_))
      subscriberIssues   = topic.subscribe(5)
                             .mapAccumulate(Set.empty[WorklogId]) {
                               case (set, worklog) =>
                                 if set(worklog.id) then (set, None)
                                 else (set + worklog.id, Some(worklog))
                             }.map(_._2)
                             .collect { case Some(w) => w }
                             .mapAsync(5)(w => importIssueDetails(w))
                             .map(Right(_))
                             .debug(_ => "issue-detail")
      result            <- subscriberWorklogs
                             .merge(subscriberIssues)
                             .fold((List.empty[Worklog], Map.empty[WorklogId, IssueDetails])) { case ((ws, m), either) =>
                               val (ws_, m_) = either match {
                                 case Left(wl)          => (wl :: ws, m)
                                 case Right(detailsMap) => (ws, m ++ detailsMap)
                               }
                               (ws_, m_)
                             }.concurrently(publisher)
    yield result

  def run: IO[Unit] = program.debug(_.toString).compile.drain

}
