//> using toolkit 0.6.0

import scala.util.chaining.scalaUtilChainingOps

object Update extends App:

  println(os.pwd)
  val path = os.pwd / os.up

  os.walk(path)
    .filter(_.ext == "scala")
    .foreach { file =>
      val a = os
        .proc("scala-cli", "dependency-update", file.toString, "--all")
        .call(cwd = path)

      println(a.exitCode + " " + file)
    }
