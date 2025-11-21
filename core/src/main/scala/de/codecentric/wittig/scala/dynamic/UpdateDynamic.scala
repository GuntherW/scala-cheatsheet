package de.codecentric.wittig.scala.dynamic

import scala.collection.mutable
import scala.language.dynamics

@main
def updateDynamic(): Unit =

  val dyn2 = new Dyn2
  dyn2.hallo = "eins"
  dyn2.hallo2 = "zwei"

  dyn2.members.foreach(println)

class Dyn2 extends Dynamic:
  val members                                          = mutable.Map.empty[String, String]
  def updateDynamic(name: String)(value: String): Unit = members(name) = value
