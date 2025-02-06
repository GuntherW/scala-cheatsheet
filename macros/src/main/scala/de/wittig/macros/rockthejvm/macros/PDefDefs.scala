package de.wittig.macros.rockthejvm.macros

import quoted.*

object PDefDefs:

  inline def generateDynamicFunction(anInt: Int, aString: String, aBoolean: Boolean): Int = ${ generateDynamicFunctionImpl('anInt, 'aString, 'aBoolean) }

  private def generateDynamicFunctionImpl(int: Expr[Int], string: Expr[String], boolean: Expr[Boolean])(using Quotes) =
    import quotes.reflect.*

    /* Syntetic code:
     *
     * def myFunction(int:Int, string:String, boolean:Boolean) {
     *  if (boolean) int else string.length
     * }
     *
     * myFunction(int, string, boolean)
     */

    // method signature
    val defSymbol = Symbol.newMethod(
      parent = Symbol.spliceOwner,
      name = "myFunction",
      tpe = MethodType(
        paramNames = List("anInt", "aString", "aBoolean"),
      )(
        paramInfosExp = _ => List(TypeRepr.of[Int], TypeRepr.of[String], TypeRepr.of[Boolean]),
        resultTypeExp = _ => TypeRepr.of[Int]
      ),
      flags = Flags.EmptyFlags,
      privateWithin = Symbol.noSymbol
    )

    // method Body
    def defBody(args: List[List[Tree]]): Option[Term] = Some {
      given Quotes = defSymbol.asQuotes // needed to make this term "owned" by the definition

      val List(List(intTerm, stringTerm, booleanTerm)) = args
      val theInt                                       = intTerm.asExprOf[Int]
      val theString                                    = stringTerm.asExprOf[String]
      val theBoolean                                   = booleanTerm.asExprOf[Boolean]

      '{ if $theBoolean then $theInt else $theString.length }
        .asTerm
        .changeOwner(defSymbol) // same as with the given Quotes
    }

    val defDef = DefDef(defSymbol, defBody)

    // using the method needs to REFER to it
    val defRef = Ref(defSymbol)

    val defUsage = defRef.appliedTo(int.asTerm, string.asTerm, boolean.asTerm)

    Block(
      List(defDef),
      defUsage
    ).asExprOf[Int]
