package de.codecentric.wittig.scala.monocle.devinsideyou

import monocle.macros.GenIso
import monocle.{Iso, Lens}

final case class Dog(name: String)
final case class Owner(name: String, pet: Dog)

object MyIso {

  type A = (Int, String, Dog)
  type B = (Int, (String, Dog))
  type C = (Int, Owner)

  val isoAB: Iso[A, B] = Iso[A, B] {
    case (int, string, dog) => (int, (string, dog))
  } {
    case (int, (string, dog)) => (int, string, dog)
  }

  val isoBC: Iso[B, C] = Iso[B, C] {
    case (int, (string, dog)) => (int, Owner(string, dog))
  } {
    case (int, Owner(string, dog)) => (int, (string, dog))
  }

  val isoAC: Iso[A, C] = isoAB composeIso isoBC
  val isoCA: Iso[C, A] = isoAC.reverse

  val isoIsALens: Lens[C, A] = isoCA.asLens

  val fields: Iso[Owner, (String, Dog)] = GenIso.fields[Owner]

  def run(): Unit = {
    println("-" * 40 + "Isos" + "-" * 40)

    val a: A = (27, "bob", Dog("Hasso"))
    println(s"A: $a")

    val c: C = isoAC.get(a)
    println(s"C: $c")

    val tuple = fields.get(Owner("Bob", Dog("Hasso")))
    println(s"tuple: $tuple")

    val owner1 = fields.reverse.get(tuple)
    val owner2 = fields(tuple)
    println(s"Owner: $owner1")
    println(s"Owner: $owner2")
  }
}
