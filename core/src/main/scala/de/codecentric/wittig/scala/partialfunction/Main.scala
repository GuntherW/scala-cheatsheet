package de.codecentric.wittig.scala.partialfunction

@main
def main(): Unit =

  val myPartialFunction: PartialFunction[Int, Int] =
    new PartialFunction[Int, Int] {
      override def isDefinedAt(x: Int): Boolean = x != 0
      override def apply(x: Int): Int           = x * 2
    }

  val optionFunction     = myPartialFunction.lift
  val myPartialFunction1 = optionFunction.unlift

  println(myPartialFunction(0))
  println(myPartialFunction(2))
  println(optionFunction(0))
  println(optionFunction(2))

  case class Person(name: String, age: Int)

  val older18: PartialFunction[Person, String] = {
    case Person(name, age) if age > 18 => name.toUpperCase()
  }
  val older12: PartialFunction[Person, String] = {
    case Person(name, age) if age > 12 => name.toLowerCase()
  }

  val combined = older18 orElse older12

  println(older18(Person("Peter", 20)))
//  println(older18(Person("Peter", 2))) // scala.MatchError
  println(combined(Person("Peter", 13)))
