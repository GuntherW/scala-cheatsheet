sealed trait Person
case class Employee(name: String, alter: Int) extends Person
case class Employer(name: String, alter: Int) extends Person

object Hallo extends App {
  println("Hallo Welt")
  println("Hallo Welt!")
}
