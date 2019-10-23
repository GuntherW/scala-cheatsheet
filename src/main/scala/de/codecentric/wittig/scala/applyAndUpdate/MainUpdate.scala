package de.codecentric.wittig.scala.applyAndUpdate

case class PersonUpdate(name: String, age: Int)

object PersonUpdate {
  def update(name: String, age: Int): PersonUpdate = PersonUpdate(name, age)
}

object MainUpdate extends App {
  val p = PersonUpdate("gunther") = 22 // Komischer Mist...
  println(p)
}
