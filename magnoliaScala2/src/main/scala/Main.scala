sealed trait Person
case class Employee(name: String, alter: Int) extends Person
case class Employer(name: String, alter: Int) extends Person
import ShowDerivation._
import Show._

object Hallo extends App {
  println(gen.show(Employer("Employer", 123)))
  println(gen.show(Employee("Employee", 333)))
  val p: Person = Employee("Person", 111)
  println(gen.show(p))

  // mit Syntax
  println(p.show)
}
