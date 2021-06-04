package de.codecentric.wittig.scala.types

/** =:= and <:< are called generalized type constraints. They allow you, from within a type-parameterized class or trait, to further constrain one of its type parameters.
  *
  * @see
  *   https://stackoverflow.com/questions/3427345/what-do-and-mean-in-scala-2-8-and-where-are-they-documented
  */
object GeneralizedTypeConstraints extends App {

  /** A =:= B means A must be exactly B A <:< B means A must be a subtype of B (analogous to the simple type constraint <:)
    */
  case class Foo[A](a: A) {
    def getStringLength(implicit evidence: A =:= String): Int = a.length
    def getName(implicit evidence: A <:< Animal): String      = a.name
  }

//  println("Stringlength: " + Foo(1).getStringLength) // does not compile because Int != String
  println("Stringlength: " + Foo("dummy").getStringLength)
  println("Name: " + Foo(Dog("Lassie")).getName)
}

sealed trait Animal {
  def name: String
}
case class Dog(name: String) extends Animal
case class Cat(name: String) extends Animal
