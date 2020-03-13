package de.codecentric.wittig.scala.zio
import zio.App
import zio.console._

object Main extends App {

  def run(args: List[String]) =
    myAppLogic.fold(_ => 1, _ => 0)

  private val myAppLogic =
    for {
      _ <- putStrLn("Hello! What is your name?")
      n <- getStrLn
      _ <- putStrLn(s"Hello, $n, good to meet you!")
    } yield ()
}
