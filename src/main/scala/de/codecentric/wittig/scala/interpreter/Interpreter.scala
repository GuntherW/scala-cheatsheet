package de.codecentric.wittig.scala.interpreter

/**
  * @author gunther
  */
object Interpreter extends App {

  import scala.reflect.runtime.currentMirror
  import scala.tools.reflect.ToolBox
  val toolbox = currentMirror.mkToolBox()

  val as    = "2*(2+3)"
  val compe = toolbox.eval(toolbox.parse(as))
  println(compe.getClass)
  println(compe)

}
