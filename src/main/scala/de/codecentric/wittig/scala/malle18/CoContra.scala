package de.codecentric.wittig.scala.malle18

class CoContra {
  trait Animal {
    def legs: Int
  }
  class Bird extends Animal {
    def legs       = 2
    def wings: Int = 2
  }
  class Fish extends Animal {
    def legs = 0
  }

  case class Box[A](a: A)
  /*
     Box[Animal]     Box[Animal]     Box[Animal]
         <:              >:
     Box[Fish]       Box[Fish]       Box[Fish]
   */
  val list: Seq[Box[Animal]] = List(
    Box(new Bird),
    Box(new Fish)
  )

  trait MyList[+A]
  case class Cons[A](head: A, tail: MyList[A]) extends MyList[A]
  case object Empty                            extends MyList[Nothing]

  trait Predicate[-A] {
    def check(input: A): Boolean
  }

  val twolegs: Predicate[Animal] = (input: Animal) => input.legs == 2

  val hasWings: Predicate[Bird] = (input: Bird) => input.wings == 2

  val ps: List[Predicate[Bird]] = List[Predicate[Bird]](twolegs, hasWings)
}
