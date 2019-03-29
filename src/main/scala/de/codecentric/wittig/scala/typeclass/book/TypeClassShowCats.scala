package de.codecentric.wittig.scala.typeclass.book
import cats.Show
import cats.syntax.show._

object DrittesBeispiel extends App {

  // Könnte man so machen
  implicit val hundShow = new Show[Dog] {
    override def show(c: Dog) = s"${c.name} is a ${c.age} year-old ${c.color} dog"
  }

  // Oder direkt so.
  implicit val katzenShow = Show.show[Cat](c => s"${c.name} is a ${c.age} year-old ${c.color} cat")

  val cat  = Cat("Tina", 243, "rot")
  val hund = Dog("Heide", 24, "braun")

  println(cat.show)
  println(hund.show)
}
