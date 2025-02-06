package de.wittig.macros.rockthejvm.inlines

object ECompileTimeOps:

  object Ints:
    import compiletime.ops.any.ToString
    import compiletime.ops.int.{ToString => _, *}

    val two: 1 + 1       = 2
    val two2: +[1, 1]    = 2
    val four: 2 * 2      = 4
    val truth: 3 <= 4    = true
    val truth2: <=[3, 4] = true

    val aString: ToString[2 * 4] = "8"

  object Booleans:
    import compiletime.ops.boolean.*

    val lie: ![true]                  = false
    val combination: true && false    = false
    val combination2: &&[true, false] = false

  object Strings:
    import compiletime.ops.string.*

    val aLiteral: "Scala"                      = "Scala"
    val aLenth: Length["Scala"]                = 5
    val regexMatch: Matches["Scala", ".*al.*"] = true

  object Values:
    import compiletime.ops.int.+
    import compiletime.ops.string.Length
    import compiletime.constValue

    val five  = constValue[3 + 2]           // Type 5 => Value 5
    val five2 = constValue[Length["Scala"]] // Type 5 => Value 5

    // Anything other than a Literal will fail.
