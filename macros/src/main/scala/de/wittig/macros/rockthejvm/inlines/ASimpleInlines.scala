package de.wittig.macros.rockthejvm.inlines

/** Der Unterschied wird im Output des Compilers klar. (mit compiler flag -Xprint:postInlining).
  *
  * Am besten diese Datei mit VSCode öffnen, da bei IntelliJ alles Neukompiliert wird.
  */
object ASimpleInlines extends App:

  def increment(x: Int): Int       = x + 1
  inline def inc(x: Int): Int      = x + 1
  inline def incia(inline x: Int)  = x + 1     // conceptually similar to by-name invocation, but at compile time
  inline def incia2(inline x: Int) = x + x + 1 // Will be inlined twice

  val number = 3
  val f      = increment(number)
  val four   = inc(number)

  val eight   = inc(2 * number + 1)
  val eight2  = incia(2 * number + 1)
  val eight22 = incia2(2 * number + 1)
