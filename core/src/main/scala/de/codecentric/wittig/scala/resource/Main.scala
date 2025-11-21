package de.codecentric.wittig.scala.resource

import java.io.{BufferedReader, FileReader}
import scala.util.Using

@main
def main(): Unit =

  mitUsing("alias.sbt")
  mitManager("alias.sbt", "build.sbt")

  def mitUsing(filePath: String) =
    Using(new BufferedReader(new FileReader(filePath))) { reader =>
      println(reader.readLine())
    }

  def mitManager(filePath1: String, filePath2: String) =
    Using.Manager { use =>
      val file1 = use(new BufferedReader(new FileReader(filePath1)))
      val file2 = use(new BufferedReader(new FileReader(filePath2)))

      println(file1.readLine())
      println(file2.readLine())
    }
