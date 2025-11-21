package de.wittig.macros.rockthejvm.inlines

@main
def bInlineConditions(): Unit =

  inline def condition1(b: Boolean): String =
    if (b) "yes"
    else "no"

  inline def condition2(b: Boolean): String =
    inline if (b) "yes"
    else "no"

  val variable   = true
  val positive1  = condition1(true)
  val positive1a = condition1(variable)
  val positive2  = condition2(true)
  // val positive2b = condition2(variable) // does not compile

  transparent inline def conditionUnion(b: Boolean): String | Int =
    if b then "yes" else 0

  val union1 = conditionUnion(true)     // String
  val union2 = conditionUnion(false)    // Int
  val union3 = conditionUnion(variable) // Int

  // recursion
  transparent inline def sum(i: Int): Int =
    inline if i == 0 then 0 else i + sum(i - 1)

  val ten: 10 = sum(4)
