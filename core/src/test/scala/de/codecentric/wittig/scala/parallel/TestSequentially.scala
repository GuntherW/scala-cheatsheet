package de.codecentric.wittig.scala.parallel

import org.scalatest.funsuite.AnyFunSuite
import scala.language.adhocExtensions

class TestSequentially extends AnyFunSuite:
  (0 to 10).foreach(i =>
    test(s"$i") {
      Thread.sleep(200)
      println(s"TestSequentially $i")
    }
  )
