package de.wittig.zio.stream

import zio._
import zio.console.{Console, putStrLn}
import zio.stream.ZStream

object HelloWorld extends App {
  def run(args: List[String]): URIO[Console, ExitCode] = programm.exitCode

  private val programm = for {
    elements <- ZStream("Hello", "World").runCollect
    _        <- putStrLn(elements.toString)
  } yield ()
}

object InfiniteStream extends App {
  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    ZStream
      .iterate(0)(_ + 1)
      .take(20)
      .runCollect
      .flatMap { chunk =>
        putStrLn(chunk.toString)
      }
      .exitCode
}

object Effect extends App {
  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    val pr = ZStream.fromEffect(putStrLn("Hello World")).drain
    val em = ZStream.iterate(0)(_ + 1).tap(i => putStrLn((i * 2).toString)).take(20)
    (pr ++ em).runCollect.exitCode
  }
}

object ControllFlow extends App {
  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    ZStream
      .repeatEffect(console.getStrLn)
      .take(5)
      .tap(line => putStrLn(line) *> putStrLn(line))
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
