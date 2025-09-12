package de.codecentric.wittig.scala.clazz

class A(a: Int):
  def other(other: A): Int = this.a // + other.a // does not compile

class B(private val a: Int):
  def other(other: B): Int = this.a + other.a

@main
def main(): Unit =
  println(B(1).other(B(2)))
