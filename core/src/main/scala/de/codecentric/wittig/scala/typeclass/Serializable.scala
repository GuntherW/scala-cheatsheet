package de.codecentric.wittig.scala.typeclass

trait Serializable[T] {
  def ser(t: T): String
}

object Serializable {
  implicit object PersonSerializer extends Serializable[Person] {
    def ser(t: Person) = {
      s"${t.name} : ${t.alter}"
    }
  }

  implicit object HalloSerializer extends Serializable[Hallo] {
    def ser(t: Hallo) = {
      s"${t.value}"
    }
  }
}

trait Serializer {
  def serialize[T](t: T)(implicit serializable: Serializable[T]) = {
    serializable.ser(t)
  }

  // Hier noch ein zusätzliches Bonbon, um zusätzlich zu serialize(p) ein p.serialisiere zuzulassen.
  implicit def addSerializable[T](t: T)(implicit serializable: Serializable[T]) = new {
    def serialisiere = serializable.ser(t)
  }
}

object Main extends App with Serializer {
  val person = new Person("Henning", 3)
  val hallo  = Hallo("wert")

  println(serialize(person))
  println(hallo.serialisiere)
  println(person.serialisiere)
}

case class Person(name: String, alter: Int)
case class Hallo(value: String)
