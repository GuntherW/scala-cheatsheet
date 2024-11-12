package de.codecentric.wittig.scala.dynamic

import scala.language.dynamics

object ApplyDynamicNamed extends App:

  println(Dyn4.foo(eins = 99, zwei = false))
  println(Dyn4.foo(99, zwei = false))

object Dyn4 extends Dynamic:
  def applyDynamicNamed(name: String)(args: (String, Any)*): String =
    val attributes = args
      .map { case (k, v) => s"""$k="${v.toString}"""" }
      .mkString(" ")
    s"<$name $attributes></$name>"
