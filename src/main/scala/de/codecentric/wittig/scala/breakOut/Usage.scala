package de.codecentric.wittig.scala.breakOut

import scala.collection.breakOut

/**
  * @author gunther
  */
object Usage {

  val list = List(1 -> 'a', 2 -> 'B')

  // Durch den Einsatz von breakOut wird hier die Erzeugung der Liste (beim map), gespart und es wird direkt die Map erzeugt.
  val map: Map[Int, Char] = list.map { case (key, value) => key -> value.toLower }(breakOut)

}
