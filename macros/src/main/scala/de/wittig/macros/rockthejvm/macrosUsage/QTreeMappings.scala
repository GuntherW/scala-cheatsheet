package de.wittig.macros.rockthejvm.macrosUsage
import de.wittig.macros.rockthejvm.macros.QTreeMappings.*

@main
def qTreeMappings(): Unit =

//   val scopedValue = transformCode {
//     def multiply(x: String, y: Int) = x * y

//     val mol = 42
//     val fl  = "Scala"

//     println(s"The meaning of life is $mol and fav language is $fl")
//   }

  val flippedBooleans = flipBooleans {
    val x = true
    val y = false
    val z = x && y

    def funcBool(a: Boolean, b: Boolean) = a && b

    if (z || false) funcBool(x, y) else false
  }
