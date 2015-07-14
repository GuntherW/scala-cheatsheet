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
  println(aa)
  println(aa.getClass)
  println(showCode(aa))
  val eval = toolbox.eval(aa)
  println(eval)
  val comp = toolbox.compile(aa)
  println(comp())

  val as = "2*(2+3)"
  val compe = toolbox.eval(toolbox.parse(as))
  println(compe)

}
