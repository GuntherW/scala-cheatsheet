package de.codecentric.wittig.scala.quasiqoutes

import scala.tools.reflect.ToolBox
import scala.reflect.quasiquotes.Quasiquotes

/**
 * @author gunther
 */
object Quoutes extends App {
  val universe: scala.reflect.runtime.universe.type = scala.reflect.runtime.universe
  import universe._
  import scala.reflect.runtime.currentMirror
  import scala.tools.reflect.ToolBox
  val toolbox = currentMirror.mkToolBox()

  val aa = q"2*(2+3)"
  println(s"""q"2*(2+3)" -> $aa""")
  println(s"getClass: ${aa.getClass}")
  println(s"showCode: ${showCode(aa)}")

  val eval = toolbox.eval(aa)
  println(s"eval: $eval")

  val comp = toolbox.compile(aa)
  println(comp())

  //
  println(toolbox.eval(toolbox.parse("2*(2+4)")))
  println(toolbox.eval(q"2*(2+5)"))

}
