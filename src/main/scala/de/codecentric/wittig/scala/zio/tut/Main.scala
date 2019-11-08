package de.codecentric.wittig.scala.zio.tut

import de.codecentric.wittig.scala.zio.tut.X
import zio._
import zio.console.Console

object Main extends App {
  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = program

  val logic: ZIO[Console with ConfigurationModule with XModule, Nothing, Int] = (for {
    _      <- console.putStrLn(s"I'm running!")
    x      <- XModule.factory.x
    _      <- console.putStrLn(s"I've got an $x!")
    config <- ConfigurationModule.factory.configuration
    _      <- console.putStrLn(s"From application.conf: ${config.eins.inner.wert}")
  } yield 0)
    .catchAll(e => console.putStrLn(s"Application run failed $e").as(1))

  private val program = logic
    .provideSome[Console] { c =>
      new Console with XModule.Live with ConfigurationModule.Live {
        override val console: Console.Service[Any] = c.console
        override val xInstance: X                  = X()
      }
    }
}
