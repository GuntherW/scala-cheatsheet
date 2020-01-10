package de.codecentric.wittig.scala.catsRelated
import cats.implicits._
import de.codecentric.wittig.scala.Implicits.RichFuture

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Applicatives extends App {
  val iiAdd: (Int, Int) => Int = (i: Int, j: Int) => i + j

  val f2 = (Future { Thread.sleep(100); println(1); 1 }, Future { Thread.sleep(10); println(2); 2 })

  println(f2.mapN(iiAdd).await)
}
