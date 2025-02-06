package de.wittig.macros.rockthejvm.macrosUsage
import de.wittig.macros.rockthejvm.macros.GSummoning.*

object GSummoning extends App:
  given MyTypeClass[String] with
    def message: String = "I am a String"

  val aTupleDescriptor = describeType[(Int, String, Boolean)]

  // val wrongTupleDescriptor = describeType[(Int, Int, Int)] // will not compile
