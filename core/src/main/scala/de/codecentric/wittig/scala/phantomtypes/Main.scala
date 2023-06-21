package de.codecentric.wittig.scala.phantomtypes

object Main extends App:

  case class StrangeBox[A](name: String)

  val box = StrangeBox[Int]("Hallo")
  print(box.name) // "cremains"

  def stringBoxesOnly(box: StrangeBox[String]): Unit = ???

//  stringBoxesOnly(box)
