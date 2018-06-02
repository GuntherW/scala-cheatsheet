package de.codecentric.wittig.scala.dynamictyping

import scala.language.dynamics

/*
 *
 * Doku im Javadoc im Trait Dynamic
 */
object Dyn extends App {

  class A extends Dynamic {
    def method(i: Int): String = i.toString

    def applyDynamic(name: String)(number: Int): Int = name match {
      case "someOtherMethod" => 2 * number
      case "someOther"       => 4 * number
      case _                 => throw new RuntimeException(s"Method $name not implemented!")
    }

    def selectDynamic(s: String) = s match {
      case "a" => "Hallo"
      case "b" => "Hallo B"
      case _   => throw new RuntimeException(s"Method $s not implemented")
    }
  }

  val x = new A
  x.someOther(5) // applyDynamic
  println(x.b) // selectDymamic
}
