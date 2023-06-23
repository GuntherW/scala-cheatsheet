package de.codecentric.wittig.scala.phantomtypes

object SimpleExample extends App:

  private val box = StrangeBox[Int]("Hallo")
  print(box.name) // "Hallo"

  def stringBoxesOnly(box: StrangeBox[String]): Unit = ???
//  stringBoxesOnly(box) // does not compile

case class StrangeBox[A](name: String)
