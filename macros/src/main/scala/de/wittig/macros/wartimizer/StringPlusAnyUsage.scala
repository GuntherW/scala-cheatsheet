package de.wittig.macros.wartimizer
import de.wittig.macros.wartimizer.Wartimizer.wartimize
case class Person(name: String, email: String)

object StringPlusAnyUsage extends App:

  val badPractice = "This is Scala: " + Person("a", "a@b.de")

  val linted1 = wartimize(StringPlusAny)(badPractice)

  // this does not compile
  // val linted2 = wartimize(StringPlusAny)("This is Scala: " + Person("a", "a@b.de"))

  println(badPractice)
