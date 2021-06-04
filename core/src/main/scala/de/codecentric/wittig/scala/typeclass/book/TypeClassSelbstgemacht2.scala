package de.codecentric.wittig.scala.typeclass.book

/** @see
  *   https://scalac.io/typeclasses-in-scala/
  */
trait Show[A] {
  def show(a: A): String
}

object Show {
  // Damit ist die Benutzung z.B. mit Show[Int].show(2) möglich
  def apply[A](implicit sh: Show[A]): Show[A] = sh

  // Ab in eigenes Objekt, damit man die Syntax unabhängig vom Rest "import"ieren kann. (Vermeidung von "ambiguous implicits")
  object ops {
    // Eine Api für den User
    def show[A: Show](a: A): String = Show[A].show(a)

    // Syntax
    implicit class ShowOps[A: Show](a: A) {
      def show = Show[A].show(a)
    }
  }

  // Helfermethode zur Erzeugung von Typeclassinstanzen
  def instance[A](func: A => String): Show[A] =
    new Show[A] {
      def show(a: A): String = func(a)
    }

  implicit val intCanShow: Show[Int] =
    instance(int => s"int $int")

  implicit val stringCanShow: Show[String] =
    instance(str => s"string $str")
}

object TypeClassSelbstgemacht2 extends App {
  import Show._
  import Show.ops._

  show(1)
  Show[Int].show(1)

  println(1.show)
  println("1".show)
}
