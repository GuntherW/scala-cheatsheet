package de.codecentric.wittig.scala.typeclass.book

/**   1. Die Type Class. Ein Trait mit der gewünschten API
  */
trait Printable[A] {
  def format(a: A): String
}

/** 2. Ein Objekt mit Instanzen für alle gewünschten Typen
  */
object PrintDefaults {
  implicit val printableInt    = new Printable[Int] {
    def format(i: Int) = i.toString
  }
  implicit val printableString = new Printable[String] {
    def format(i: String) = i
  }
  implicit val printableCat    = new Printable[Cat] {
    def format(i: Cat) = s"${i.name} is a ${i.age} year-old ${i.color} cat"
  }
}

/** 3. Eine Api für den User
  */
object Print {
  def format[A](a: A)(implicit p: Printable[A]): String = p.format(a)
  def print[A](a: A)(implicit p: Printable[A]): Unit    = println(p.format(a))
}

/** 4. (optional) Eine Einrichment Klasse.
  */
object PrintSyntax {
  implicit class PrintOps[A](value: A) {
    def print(implicit p: Printable[A])  = println(p.format(value))
    def format(implicit p: Printable[A]) = p.format(value)
  }
}

object TypeClassSelbstgemacht extends App {
  import PrintDefaults._
  import PrintSyntax._

  // Benutzung über das Objekt
  Print.print(1)

  // Benutzung über die implizite "Enrich"-Klasse
  112.print

  val cat = Cat("Hansi", 11, "braun")
  cat.print
}
