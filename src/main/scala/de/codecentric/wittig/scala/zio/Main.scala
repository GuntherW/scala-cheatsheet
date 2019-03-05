package de.codecentric.wittig.scala.zio
import scalaz.zio.App
import scalaz.zio.console._

object Main extends App {

  def run(args: List[String]) =
    myAppLogic.either
      .map(_.fold(_ => 1, _ => 0))

  val myAppLogic =
    for {
      _ <- putStrLn("Hello! What is your name?")
      n <- getStrLn
      _ <- putStrLn(s"Hello, ${n}, good to meet you!")
    } yield ()
}
