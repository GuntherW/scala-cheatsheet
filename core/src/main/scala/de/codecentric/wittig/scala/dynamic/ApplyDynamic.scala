package de.codecentric.wittig.scala.dynamic

import scala.language.dynamics

object ApplyDynamic extends App:

  private val dyn = new Dyn3

  println(dyn.foo(99, false))

class Dyn3 extends Dynamic:
  def applyDynamic(name: String)(args: Any*): Seq[(String, Any)] = args.map(arg => name -> arg)
