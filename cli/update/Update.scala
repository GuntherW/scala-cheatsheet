//> using toolkit 0.6.0

import scala.util.chaining.scalaUtilChainingOps
import scala.util.{Failure, Success, Try}

@main
def update(): Unit =

  println(os.pwd)
  val path = os.pwd / os.up

  os.walk(path)
    .filter(_.ext == "scala")
    .foreach { file =>
      Try(
        os
          .proc("scala-cli", "dependency-update", file.toString, "--all")
          .call(cwd = path)
      ) match
        case Success(result) => println(s"${result.exitCode} $file")
        case Failure(error)  => println(s"Fehler bei $file: ${error.getMessage}")
    }
