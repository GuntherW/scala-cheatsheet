package de.codecentric.wittig.scala.dynamic

import scala.language.dynamics

object SelectDynamic extends App:

  private val dyn = Dyn(Map("foo" -> "fooValue", "bar" -> "barValue"))

  println(dyn.foo)
  println(dyn.bar)
//  println(dyn.bat) NoSuchElementException

class Dyn(members: Map[String, String]) extends Dynamic:
  val foo                                 = "hallo"
  def selectDynamic(name: String): String = members(name)
