package de.codecentric.wittig.scala.zio
import java.io.IOException

import zio.{App, ZIO}
import zio.console._

object Main extends App {
  def run(args: List[String]) =
    myAppLogic.either
      .map(_.fold(_ => 1, _ => 0))

  private val myAppLogic: ZIO[Console, IOException, Unit] =
    for {
      _ <- putStrLn("Hello! What is your name?")
      n <- getStrLn
      _ <- putStrLn(s"Hello, ${n}, good to meet you!")
    } yield ()
}
