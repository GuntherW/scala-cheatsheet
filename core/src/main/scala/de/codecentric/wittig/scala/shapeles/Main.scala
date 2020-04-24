package de.codecentric.wittig.scala.shapeles
import shapeless.{LabelledGeneric, _}

case class Person(name: String, alter: Int, beruf: Option[String])
object Person {
  implicit val gen = LabelledGeneric[Person]
}

object Main extends App {
  case class Employee(name: String, number: Int, manager: Boolean)
  case class IceCream(name: String, numCherries: Int, inCone: Boolean)

  def employeeCsv(e: Employee): List[String] =
    List(e.name, e.number.toString, e.manager.toString)
  def iceCreamCsv(c: IceCream): List[String] =
    List(c.name, c.numCherries.toString, c.inCone.toString)

  val gen1 = Generic[Employee].to(Employee("guntherd", 22, true))
  val gen2 = Generic[IceCream].to(IceCream("Schoko", 22, true))
  println(gen1)
  println(gen2)
}
