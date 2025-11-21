package de.wittig.macros.rockthejvm.inlines

@main
def dCompileTimeErrors(): Unit =

  inline def compileTimeError(x: Int): Int =
    compiletime.error("This is a compile-time error")

  // val three = compileTimeError(two) // does not compile

  inline def pmWithCTError(x: Option[Any]): String =
    inline x match
      case Some(v: Int)    => v.toString
      case Some(v: String) => v
      case None            => "nothing"
      case _               => compiletime.error("This value is not supported")

  val one  = pmWithCTError(Some(1))
  val two  = pmWithCTError(Some("two"))
  val none = pmWithCTError(None)
// val error = pmWithCTError(Some(2.0)) // does not compile
