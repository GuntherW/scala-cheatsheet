package de.codecentric.wittig.scala.scalazstuff

import scalaz._
import Scalaz._

/**
  * @author gunther
  */
object ScalazsEither extends App {

  def age(s: String): String \/ Int = s match {
    case "Erik" => \/-(30)
    case other  => -\/(s"name of $other is unknown")
  }

  println(age("Erik")) // \/-(30)

  println(age("John")) // -\/(name of John is unknown)

  // das Scalaz Either bietet auch map und flatmap an. Somit sind auch for comprehensions möglich:

  val totalAge = for {
    x <- age("Erik")
    y <- age("John")
    z <- age("Mary")
  } yield x + y + z
  println(totalAge)
  // -\/(name of John is unknown)

}
