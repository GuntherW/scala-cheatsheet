package de.wittig.macros.baeldung

import scala.quoted.{Expr, Quotes}
import scala.util.chaining.scalaUtilChainingOps
import InlineExample.*
import MacroExample.*

object ExampleMain extends App:

  oddEvenMacroInline(1).tap(println)
  oddEvenMacroInlineConditional(1).tap(println)
  oddEvenMacroTransparent(1).tap(println)
  oddEvenMacroQuote(1).tap(println)

  println("-" * 10)

  oddEvenMacroInline(2).tap(println)
  oddEvenMacroInlineConditional(2).tap(println)
  oddEvenMacroTransparent(2).tap(println)
  oddEvenMacroQuote(2).tap(println)

  println("-" * 10)

  private val zahl1 = 1
  oddEvenMacroInline(zahl1).tap(println)
  oddEvenMacroInlineConditional(zahl1).tap(println)
  oddEvenMacroTransparent(zahl1).tap(println)
  oddEvenMacroQuote(zahl1).tap(println)

  println("-" * 10)

  private val zahl2 = 2
  oddEvenMacroInline(zahl2).tap(println)
  oddEvenMacroInlineConditional(zahl2).tap(println)
  oddEvenMacroTransparent(zahl2).tap(println)
  oddEvenMacroQuote(zahl2).tap(println)

  println("-" * 10)
