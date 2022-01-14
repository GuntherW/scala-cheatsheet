package de.wittig.zio.stream

import zio.*
import zio.Console.*
import zio.stream.ZStream
import zio.Duration.*

object HelloWorld extends App {
  def run(args: List[String]): URIO[Console, ExitCode] = programm.exitCode

  private val programm = for {
    elements <- ZStream("Hello", "World").runCollect
    _        <- printLine(elements.toString)
  } yield ()
}

object InfiniteStream extends App {
  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    ZStream
      .iterate(0)(_ + 1)
      .take(20)
      .runCollect
      .flatMap { chunk =>
        printLine(chunk.toString)
      }
      .exitCode
}

object Effect extends App {
  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    val pr = ZStream.fromZIO(printLine("Hello World")).drain
    val em = ZStream.iterate(0)(_ + 1).tap(i => printLine((i * 2).toString)).take(20)
    (pr ++ em).runCollect.exitCode
  }
}

object ControllFlow extends App {
  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    ZStream
      .repeatZIO(readLine)
      .take(5)
      .tap(line => printLine(line) *> printLine(line))
      .runCollect
      .exitCode
}

object Transforming extends App {
  case class StockQoute(symbol: String, openPrice: Double, closePrice: Double)

  private val streamStocks       = ZStream(StockQoute("DDOG", 37.123, 34.123), StockQoute("NET", 35.123, 37.123))
  private val streamOpenAndClose = streamStocks.flatMap {
    case StockQoute(symbol, open, close) =>
      ZStream(
        symbol -> open,
        symbol -> close
      )
  }

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    streamOpenAndClose.runCollect.exitCode
}
