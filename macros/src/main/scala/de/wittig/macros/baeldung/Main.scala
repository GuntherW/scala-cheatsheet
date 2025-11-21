package de.wittig.macros.baeldung

import scala.util.chaining.scalaUtilChainingOps

import de.wittig.macros.baeldung.Inliners.*
import de.wittig.macros.baeldung.Macros.*

@main
def main(): Unit =

  println("-" * 10 + "1")
  oddEvenInline(1).tap(println)
  oddEvenInlineArgument(1).tap(println)
  oddEvenInlineInlineMatch(1).tap(println)
  oddEvenTransparent(1).tap(println)
  oddEvenQuote(1).tap(println)
  println("-" * 10 + "1")

  println("-" * 10 + "2")
  oddEvenInline(2).tap(println)
  oddEvenInlineArgument(2).tap(println)
  oddEvenInlineInlineMatch(2).tap(println)
  oddEvenTransparent(2).tap(println)
  oddEvenQuote(2).tap(println)
  println("-" * 10 + "2")

  val zahl1 = 1
  println("-" * 10 + "private val zahl1 = 1")
  oddEvenInline(zahl1).tap(println)
  oddEvenInlineArgument(zahl1).tap(println)
  oddEvenInlineInlineMatch(zahl1).tap(println)
  oddEvenTransparent(zahl1).tap(println)
  oddEvenQuote(zahl1).tap(println)
  println("-" * 10 + "private val zahl1 = 1")

  val zahl2 = 2
  println("-" * 10 + "private val zahl2 = 2")
  oddEvenInline(zahl2).tap(println)
  oddEvenInlineArgument(zahl2).tap(println)
  oddEvenInlineInlineMatch(zahl2).tap(println) // Fehler: Wegen des Inline matches. Nur Konstanten sind als Übergabeparameter erlaubt.
  oddEvenTransparent(zahl2).tap(println)       // Fehler: siehe oben
  oddEvenQuote(zahl2).tap(println)
  println("-" * 10 + "private val zahl2 = 2")

  inline val zahl1Inline = 1
  println("-" * 10 + "private inline val zahl1Inline = 1")
  oddEvenInline(zahl1Inline).tap(println)
  oddEvenInlineArgument(zahl1Inline).tap(println)
  oddEvenInlineInlineMatch(zahl1Inline).tap(println)
  oddEvenTransparent(zahl1Inline).tap(println)
  oddEvenQuote(zahl1Inline).tap(println)
  println("-" * 10 + "private inline val zahl1Inline = 1")

  inline val zahl2Inline = 2
  println("-" * 10 + "private inline val zahl2Inline = 2")
  oddEvenInline(zahl2Inline).tap(println)
  oddEvenInlineArgument(zahl2Inline).tap(println)
  oddEvenInlineInlineMatch(zahl2Inline).tap(println)
  oddEvenTransparent(zahl2Inline).tap(println)
  oddEvenQuote(zahl2Inline).tap(println)
  println("-" * 10 + "private inline val zahl2Inline = 2")
