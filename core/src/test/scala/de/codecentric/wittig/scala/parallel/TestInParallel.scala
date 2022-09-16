package de.codecentric.wittig.scala.parallel

import org.scalatest.ParallelTestExecution
import org.scalatest.funsuite.AnyFunSuite
import scala.language.adhocExtensions

/** Um Tests parallel laufen zu lasssen, einfach vom Trait [[ParallelTestExecution]] extenden.
  */
class TestInParallel extends AnyFunSuite with ParallelTestExecution:
  (0 to 10).foreach(i =>
    test(s"$i") {
//      Thread.sleep(500)
      println(s"TestInParallel $i")
    }
  )
