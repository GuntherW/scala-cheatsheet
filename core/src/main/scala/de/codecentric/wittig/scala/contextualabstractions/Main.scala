package de.codecentric.wittig.scala.contextualabstractions

object Main extends App:

  // simple
  given defaultInt: Int = 10

  def add(x: Int)(using y: Int) = x + y
  println(add(1)) // 11

  // more complex
  trait Combiner[A]:
    def combine(x: A, y: A): A
    def empty: A

  given intCombiner: Combiner[Int] with {
    override def combine(x: Int, y: Int): Int = x + y
    override def empty: Int                   = 0
  }

  def combineAll[A](l: List[A])(using combiner: Combiner[A]) = l.foldLeft(combiner.empty)(combiner.combine)

  val numbers = (1 to 10).toList
  println(combineAll(numbers))

  // syntesize given instances
  given optionCombiner[T](using combiner: Combiner[T]): Combiner[Option[T]] with {
    override def combine(x: Option[T], y: Option[T]): Option[T] = for {
      vx <- x
      vy <- y
    } yield combiner.combine(vx, vy)
    override def empty: Option[T]                               = Some(combiner.empty)
  }

  println(combineAll(List(Option(1), Option(2))))   // Some(3)
  println(combineAll(List(Some(1), Some(2), None))) // None

  // extension methods
  case class Person(name: String) {
    def greet(): String = s"Hallo $name"
  }
  extension (name: String)
    def greet(): String = Person(name).greet()
  println("Peter".greet())

  // type classes
  extension [T](list: List[T])
    def reduceAll(using combiner: Combiner[T]) =
      list.foldLeft(combiner.empty)(combiner.combine)

  println(numbers.reduceAll)
