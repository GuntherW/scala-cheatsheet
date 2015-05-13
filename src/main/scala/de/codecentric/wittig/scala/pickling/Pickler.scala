package de.codecentric.wittig.scala.pickling
import scala.pickling.Defaults._

object Pickler extends App {

  case class Person(name: String, alter: Int, zeitreisender: Boolean, adresse: Adresse)
  case class Adresse(strasse: String, plz: Int)

  val p = Person("Gunther", 37, false, Adresse("Overath", 111))
  val d = Person("Dani", 33, true, Adresse("Overath", 111))

  def serializeToJson(p: Person) = {
    import scala.pickling.json._
    val json = p.pickle
    println(json)
    val zurueck = json.unpickle[Person]
    println(zurueck)
  }
  def serializeToBinary(p: Person) = {
    import scala.pickling.binary._
    val bin = p.pickle
    println(bin)
    val zurueck = bin.unpickle[Person]
    println(zurueck)
  }
  serializeToJson(p)
  serializeToBinary(p)
}
