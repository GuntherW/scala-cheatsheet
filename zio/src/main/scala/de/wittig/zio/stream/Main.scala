package de.wittig.zio.stream

import shapeless.ops.hlist.ZipOne
import zio._
import zio.console.putStrLn
import zio.random.Random
import zio.stream.ZStream

object Types {

  val oneValue: ZIO[Any, Nothing, Int]          = ZIO.succeed(1)
  val oneFailure: ZIO[Any, Throwable, Nothing]  = ZIO.fail(new RuntimeException)
  val requiresRandom: ZIO[Random, Nothing, Int] = random.nextInt

  val threeValues: ZStream[Any, Nothing, Int]        = ZStream(1, 2, 3)
  val empty: ZStream[Any, Nothing, Nothing]          = ZStream.empty
  val valueThenFailure: ZStream[Any, Throwable, Int] = ZStream(1, 2) ++ ZStream.fail(new RuntimeException)

}
object HelloWorld extends App {
  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    for {
      elements <- ZStream("Hello", "World").runCollect
      _        <- putStrLn(elements.toString)
    } yield ExitCode.success
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

  val streamStocks       = ZStream(StockQoute("DDOG", 37.123, 34.123), StockQoute("NET", 35.123, 37.123))
  val streamSymbols      = streamStocks.map(_.symbol)
  val streamOpenAndClose = streamStocks.flatMap {
    case StockQoute(symbol, open, close) =>
      ZStream(
        symbol -> open,
        symbol -> close
      )
  }

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    streamOpenAndClose.runCollect.exitCode
  }
}

object Transducing extends App {
  val stockQuotePath = ""
  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    ???
  }
}
