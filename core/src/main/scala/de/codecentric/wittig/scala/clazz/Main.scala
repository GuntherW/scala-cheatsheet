package de.codecentric.wittig.scala.clazz

class Eins(a: Int):
  def other(other: Eins): Int = this.a // + other.a // does not compile
class Zwei(private val a: Int):
  def other(other: Zwei): Int = this.a + other.a

object Main extends App:
  val zwei = Zwei(1)
  println(zwei.other(Zwei(2)))
