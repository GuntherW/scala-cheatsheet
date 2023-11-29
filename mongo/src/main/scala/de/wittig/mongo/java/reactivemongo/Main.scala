package de.wittig.mongo.java.reactivemongo

import reactivemongo.api.bson.{document, BSONDocumentReader, BSONDocumentWriter, Macros}
import reactivemongo.api.{AsyncDriver, Cursor, MongoConnection}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContext, Future} // use any appropriate context

object Main extends App:
  println("Hallo Welt")
  private val p1 = Person("Hans", "Wurst", 42, Address("Hauptstrasse 1", "Berlin"))
  private val p2 = Person("Petra", "Wursta", 41, Address("Hauptstrasse 2", "Berlin"))

  Repository.createPerson(p1).await
  Repository.createPerson(p2).await
  Repository.updatePerson(p2.copy(age = 123)).await

object Repository:
  private val mongoUri  = "mongodb://localhost:27017/"
  private val driver    = AsyncDriver()
  private val parsedUri = MongoConnection.fromString(mongoUri)

  private val connection             = parsedUri.flatMap(driver.connect)
  private lazy val dadDb             = connection.flatMap(_.database("reactive-mongo-db"))
  private lazy val collectionPersons = dadDb.map(_.collection("person"))

  // use personWriter
  def createPerson(person: Person): Future[Unit] =
    collectionPersons.flatMap(_.insert.one(person).map { _ => })

  def updatePerson(person: Person): Future[Int] = {
    val selector = document(
      "firstName" -> person.firstName,
      "lastName"  -> person.lastName
    )
    collectionPersons.flatMap(_.update.one(selector, person).map(_.n))
  }

  def findPersonByAge(age: Int): Future[List[Person]] =
    collectionPersons.flatMap(_.find(document("age" -> age))
      .cursor[Person]()
      .collect[List](-1, Cursor.FailOnError[List[Person]]()))

// Custom persistent types
case class Person(firstName: String, lastName: String, age: Int, address: Address)
object Person:
  given BSONDocumentWriter[Person] = Macros.writer[Person]
  given BSONDocumentReader[Person] = Macros.reader[Person]

case class Address(street: String, city: String)
object Address:
  given BSONDocumentWriter[Address] = Macros.writer[Address]
  given BSONDocumentReader[Address] = Macros.reader[Address]

extension [T](future: Future[T])
  def await(using duration: Duration = 10.seconds): T = Await.result(future, duration)
