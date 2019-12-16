package de.codecentric.wittig.scala.adventofcode

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}

import scala.concurrent.ExecutionContext.Implicits.global

object Advent1 extends App {

  implicit val system: ActorSystem             = ActorSystem("Advent1")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def l(): Iterator[Char] = scala.io.Source.fromFile("input.txt").getLines.flatMap(_.toArray)

  val collectDouble = Flow[Char].statefulMapConcat { () =>
    var firstChar: Option[Char] = None
    val lastChar: Option[Char]  = None
    var listOfRes               = List.empty[Char]

    currentChar =>
      if (firstChar.isEmpty)
        firstChar = Some(currentChar)

      if (lastChar.contains(currentChar))
        listOfRes = currentChar :: listOfRes

      listOfRes
  }

  val source = akka.stream.scaladsl.Source
    .fromIterator(l)
    .via(collectDouble)
    .map(_.toLong)
    .fold(0L)(_ + _)

  val sink = Sink.foreach(println)

  source
    .runWith(sink)
    .foreach(_ => system.terminate)
}
