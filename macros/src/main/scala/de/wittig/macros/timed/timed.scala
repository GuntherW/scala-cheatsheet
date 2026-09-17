package de.wittig.macros.timed

import scala.annotation.{MacroAnnotation, experimental}
import scala.quoted.*

/** Macro annotation that wraps a method body with timing logic.
  *
  * Usage:
  * {{{
  * @timed
  * def slowComputation(n: Int): Long = (1 to n).map(_.toLong).sum
  * }}}
  *
  * Generates:
  * {{{
  * def slowComputation(n: Int): Long =
  *   val __start   = System.nanoTime()
  *   val __result  = (1 to n).map(_.toLong).sum
  *   val __elapsed = System.nanoTime() - __start
  *   println("slowComputation took " + (__elapsed / 1_000_000) + "ms (" + __elapsed + "ns)")
  *   __result
  * }}}
  */
@experimental
class timed extends MacroAnnotation:

  def transform(using Quotes)(
    definition: quotes.reflect.Definition,
    companion: Option[quotes.reflect.Definition]
  ): List[quotes.reflect.Definition] =
    import quotes.reflect.*

    definition match
      case DefDef(name, params, returnTpt, Some(body)) =>
        val owner = definition.symbol

        val longType   = defn.LongClass.typeRef
        val resultType = returnTpt.tpe

        val systemClass  = Symbol.requiredClass("java.lang.System")
        val systemMod    = systemClass.companionModule
        val nanoTimeSym  = systemMod.methodMember("nanoTime").headOption
          .getOrElse(systemClass.methodMember("nanoTime").head)
        def callNanoTime = Apply(Select(Ref(systemMod), nanoTimeSym), Nil)

        val predefModule = Symbol.requiredModule("scala.Predef")
        val printlnSym   = predefModule.methodMember("println").find(_.signature.paramSigs.size == 1).get

        val longMinusSym = defn.LongClass.methodMember("-").find(_.signature.paramSigs == List("scala.Long")).get
        val longDivSym   = defn.LongClass.methodMember("/").find(_.signature.paramSigs == List("scala.Long")).get
        val strPlusSym   = defn.StringClass.methodMember("+").find(_.signature.paramSigs == List("java.lang.Object")).get

        def longMinus(a: Term, b: Term) = Apply(Select(a, longMinusSym), List(b))
        def longDiv(a: Term, b: Term)   = Apply(Select(a, longDivSym), List(b))
        def strCat(a: Term, b: Term)    = Apply(Select(a, strPlusSym), List(b))

        // val __start = System.nanoTime()
        val startSym = Symbol.newVal(owner, "__start", longType, Flags.EmptyFlags, Symbol.noSymbol)
        val startDef = ValDef(startSym, Some(callNanoTime))
        val startRef = Ref(startSym)

        // val __result: ReturnType = <body>
        val resultSym = Symbol.newVal(owner, "__result", resultType, Flags.EmptyFlags, Symbol.noSymbol)
        val resultDef = ValDef(resultSym, Some(body))
        val resultRef = Ref(resultSym)

        // val __elapsed = System.nanoTime() - __start
        val elapsedSym = Symbol.newVal(owner, "__elapsed", longType, Flags.EmptyFlags, Symbol.noSymbol)
        val elapsedDef = ValDef(elapsedSym, Some(longMinus(callNanoTime, startRef)))
        val elapsedRef = Ref(elapsedSym)

        // "<name> took " + (__elapsed / 1_000_000) + "ms (" + __elapsed + "ns)"
        val ms  = longDiv(elapsedRef, Literal(LongConstant(1_000_000L)))
        val msg = strCat(strCat(strCat(strCat(
          Literal(StringConstant(s"$name took ")), ms),
          Literal(StringConstant("ms ("))), elapsedRef),
          Literal(StringConstant("ns)")))

        val printStmt = Apply(Select(Ref(predefModule), printlnSym), List(msg))

        val newBody = Block(List(startDef, resultDef, elapsedDef, printStmt), resultRef)

        List(DefDef.copy(definition)(name, params, returnTpt, Some(newBody)))

      case _ =>
        report.error("@timed can only be applied to def methods")
        List(definition)
