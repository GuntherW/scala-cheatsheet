package de.wittig.macros.rockthejvm.inlines

import scala.compiletime.{summonFrom, summonInline}

object InlineSummoning extends App:
  trait Semigroup[A]:
    def combine(x: A, y: A): A
  given Semigroup[Int] = _ + _

  def doubleSimple[A](a: A)(using Semigroup[A]): A =
    summon[Semigroup[A]].combine(a, a)

  // inline summon
  inline def double[A](a: A): A =
    summonInline[Semigroup[A]].combine(a, a)

  val f1 = doubleSimple(21)
  val f2 = double(21)

  // conditional summon
  trait Messenger[A]:
    def message: String
  given Messenger[Int] with           {
    def message = "This is an Int"
  }
  trait AnotherMessenger[A]:
    def anotherMessage: String
  given AnotherMessenger[String] with {
    def anotherMessage = "This is an String"
  }

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
