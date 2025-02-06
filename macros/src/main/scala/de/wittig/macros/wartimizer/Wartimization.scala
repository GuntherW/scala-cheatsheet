package de.wittig.macros.wartimizer
import quoted.*
import scala.reflect.NameTransformer

/*
 Code processor that will transform code for ONE use-case
 Code Optimizer
 Before: List(1,2,3).filter(_ % 2 == 0).headOption
 After:  List(1,2,3).find(_ % 2 == 0)

 Wart remover
 "scala" + Person("a", "a@b.de")
 should not compile
 */
trait Wartimization { self: Singleton => // self, weil: all Wartimization instances must be objects, so that they can be referred to as constants
  def treeMap(using q: Quotes): q.reflect.TreeMap
}

object Wartimization:
  given FromExpr[Wartimization] with {
    /*
    Will fetch the Wartimization object from the classpath by its name
    => will use RUNTIME reflection
     */
    def unapply(x: Expr[Wartimization])(using Quotes): Option[Wartimization] =
      import quotes.reflect.*
      // get the name of the Wartimization instance
      val typeSymbol = x.asTerm.tpe.typeSymbol
      if (typeSymbol.flags.is(Flags.Module)) // check if Wartimization is an object
        val fullName = typeSymbol.fullName
        Some(unsafeLoadObject(fullName))
      else
        report.errorAndAbort(s"The type [${typeSymbol.name}] does not correspond to an object.", x)

    private def unsafeLoadObject[A](name: String)(using Quotes): A =
      import quotes.reflect.*
      try {
        val clazz = Class.forName(name)

        // objects are represented in the JVM as a static field cassed MODULES
        val module = clazz.getField(NameTransformer.MODULE_INSTANCE_NAME)

        // This field is normally fetched from a class instance, here we don't have one
        val objectInstance = module.get(null)

        // final value
        objectInstance.asInstanceOf[A]
      } catch {
        case e: Throwable => report.errorAndAbort(s"Failed to load class $name.")
      }
  }
