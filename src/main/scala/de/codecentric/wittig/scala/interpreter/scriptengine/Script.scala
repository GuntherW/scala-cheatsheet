package de.codecentric.wittig.scala.interpreter.scriptengine
import javax.script.ScriptEngineManager

object Script extends App {

  val mgr = new ScriptEngineManager()
  val engine = mgr.getEngineByName("JavaScript")
  val foo = "((40+2)*5-2)"
  println(engine.eval(foo))

}
