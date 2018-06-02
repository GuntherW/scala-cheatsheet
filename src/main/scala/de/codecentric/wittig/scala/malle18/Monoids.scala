package de.codecentric.wittig.scala.malle18

trait MyMonoid[A] {
  def empty: A
  def combine(x: A, y: A): A
}

object MyMonoid {
  implicit val intMonoid: MyMonoid[Int] = new MyMonoid[Int] {
    override def empty: Int = 0

    override def combine(x: Int, y: Int): Int = x + y
  }

  val intMonoidMult: MyMonoid[Int] = new MyMonoid[Int] {
    override def empty: Int = 1

    override def combine(x: Int, y: Int): Int = x * y
  }

  // left identity: empty |+| x === x
  // right identity: x |+| empty === x
  // associativy: (x |+| y) |+| z === x |+| (y |+| z)

  implicit val stringMonoid: MyMonoid[String] = new MyMonoid[String] {
    override def empty: String = ""

    override def combine(x: String, y: String): String = x ++ y
  }

  implicit class MyMonoidOps[A: MyMonoid](val self: A) {
    def |+|(other: A) = implicitly[MyMonoid[A]].combine(self, other)
  }
}

object Foo extends App {
  import MyMonoid.MyMonoidOps
  implicit val multMonoid: MyMonoid[Int] = MyMonoid.intMonoidMult

  println(1 |+| 2 |+| 3 |+| 4)
}
