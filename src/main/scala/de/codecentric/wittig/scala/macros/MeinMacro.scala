package de.codecentric.wittig.scala.macros
import language.experimental.macros
import reflect.macros.whitebox.Context
import scala.reflect.macros.whitebox

object MeinMacro extends App {

  println("Hallo Welt")
  object DebugMacros {

    // Ohne Parameter
    def hello(): Unit = macro hello_impl

    // c:Context ist so'ne Art Compilationcontext
    def hello_impl(c: whitebox.Context)(): c.Expr[Unit] = {
      import c.universe._
      reify { println("Hello World!") }
    }

    // Mit Parameter
    def printparam(param: Any): Unit = macro printparam_impl

    def printparam_impl(c: Context)(param: c.Expr[Any]): c.Expr[Unit] = {
      import c.universe._
      reify { println(param.splice) }
    }
  }

}
