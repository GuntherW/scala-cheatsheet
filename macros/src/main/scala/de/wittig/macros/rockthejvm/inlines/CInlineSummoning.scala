package de.wittig.macros.rockthejvm.inlines

import scala.compiletime.{summonFrom, summonInline}
import scala.util.chaining.scalaUtilChainingOps

@main
def cInlineSummoning(): Unit =

  trait Semigroup[A]:
    def combine(x: A, y: A): A

  def doubleSimple[A](a: A)(using Semigroup[A]): A =
    summon[Semigroup[A]].combine(a, a)

  // inline summon
  inline def doubleInline[A](a: A): A =
    summonInline[Semigroup[A]].combine(a, a)

  given Semigroup[Int] = _ + _
  val f1               = doubleSimple(21).tap(println)
  val f2               = doubleInline(21).tap(println)

// conditional summon
@main
def conditionalSummoning(): Unit =

  trait Messenger[A]:
    def message: String
  given Messenger[Int]:
    def message = "This is an Int"

  trait AnotherMessenger[A]:
    def anotherMessage: String
  given AnotherMessenger[String]:
    def anotherMessage = "This is an String"

  inline def produceMessage[A] =
    summonFrom { // erlaubt mir mit verschiedenen Typklassen, die der Kompiler findet, zu arbeiten.
      case m: Messenger[A]        => "Found messenger: " + m.message
      case m: AnotherMessenger[A] => "Found another messenger: " + m.anotherMessage
      case _                      => "No messagenger found for this type"
    }

  val intMessage   = produceMessage[Int]
  val otherMessage = produceMessage[String]
  val noMessage    = produceMessage[Double]
  println(intMessage)
  println(otherMessage)
  println(noMessage)
