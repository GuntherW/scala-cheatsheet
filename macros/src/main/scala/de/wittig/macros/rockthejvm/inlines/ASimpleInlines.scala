package de.wittig.macros.rockthejvm.inlines

/** Der Unterschied wird im Output des Compilers klar. (mit compiler flag -Xprint:postInlining).
  *
  * Am besten diese Datei mit VSCode öffnen, da bei IntelliJ alles Neukompiliert wird.
  */
@main
def aSimpleInlines(): Unit =

  def inc(x: Int): Int                 = x + 1
  inline def incInl(x: Int): Int       = x + 1
  inline def incInlInl1(inline x: Int) = x + 1     // conceptually similar to by-name invocation, but at compile time
  inline def incInlInl2(inline x: Int) = x + x + 1 // Will be inlined twice

  val number = 3
  val f      = inc(number)
  val four   = incInl(number)

  val eight  = incInl(2 * number + 1)
  val eight2 = incInlInl1(2 * number + 1)

  val eight22 = incInlInl2(2 * number + 1)
