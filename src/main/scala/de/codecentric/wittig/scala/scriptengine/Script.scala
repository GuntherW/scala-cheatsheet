package de.codecentric.wittig.scala.scriptengine
import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;

object Script extends App {

  val mgr = new ScriptEngineManager();
  val engine = mgr.getEngineByName("JavaScript");
  val foo = "((40+2)*5-2)";
  println(engine.eval(foo));

}
