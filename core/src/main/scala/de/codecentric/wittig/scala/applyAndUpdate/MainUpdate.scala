package de.codecentric.wittig.scala.applyAndUpdate

case class PersonUpdate(name: String, age: Int)

object PersonUpdate {
  def update(name: String, age: Int): PersonUpdate = PersonUpdate(name, age)

  // ist auch mit mehr als zwei Parametern möglich
  def update(name: String, age: Int, s: String): PersonUpdate = PersonUpdate(s"$name $s", age)
}

object MainUpdate extends App {
  val p = PersonUpdate("gunther") = 22 // Komischer Mist...
  println(p)

  val p2 = PersonUpdate("gun", 22) = "ther"
  println(p2)
}
