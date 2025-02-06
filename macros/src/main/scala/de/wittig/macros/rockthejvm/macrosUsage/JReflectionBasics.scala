package de.wittig.macros.rockthejvm.macrosUsage
import de.wittig.macros.rockthejvm.macros.JReflectionBasics.*

object JReflectionBasics extends App:
  case class SimpleWrapper(x: Int):
    def magicMethod(y: Int): String = s"Ergebnis: ${x + y}"

  val meaningOfLife = 42
  val s             = SimpleWrapper(10)
  val descriptor    = callMethodDynamically(s, meaningOfLife, "magicMethod")
  // transformed (at compile time) to SimpleWrapper(10).magicMethod(meaningOfLife)
  println(descriptor) // Ergebnis: 52

  // fail
//   val descriptorFail1 = callMethodDynamically(s, meaningOfLife, "magicMethodFail")
//   val descriptorFail2 = callMethodDynamically(s, true, "magicMethod")

  /** Tuple */
  val tuple1 = createTuple[2, String]("2")
