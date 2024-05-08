package de.wittig.quill

import io.getquill.*
import io.getquill.context.jdbc.JdbcContext
import DBContext.*

import scala.util.chaining.scalaUtilChainingOps

enum DBContext:
  case PostgresContext, H2Context

case class Person(id: Int, firstName: String, lastName: String, age: Int)
case class Book(id: Int, ownerId: Int, title: String)

object Main extends App:

  val dbContext: DBContext = H2Context

  private val ctx = dbContext match {
    case PostgresContext => new PostgresJdbcContext(SnakeCase, "ctxpg")
    case H2Context       => new H2JdbcContext(SnakeCase, "ctxh2")
  }
  import ctx.*

  inline def persons              = query[Person]
  inline def books                = query[Book]
  inline def allPersons           = quote(persons)
  inline def allBooks             = quote(books)
  inline def hans                 = quote(persons.filter(_.firstName == "Hans"))
  inline def updateHans(age: Int) = quote(persons.filter(_.firstName == "Hans").update(_.age -> age))

  val p1    = Person(0, "Peter", "Pan", 88)
  val book1 = Book(0, 0, "Titel 1")
  val book2 = Book(1, 0, "Titel 2")
  transaction(
    run(persons.insertValue(lift(p1))).printColored(Console.YELLOW),
    run(persons.insertValue(Person(1, "Hans", "Franz", 88))).printColored(Console.YELLOW),
    run(books.insertValue(lift(book1))).printColored(Console.YELLOW),
    run(books.insertValue(lift(book2))).printColored(Console.YELLOW),
  )
  run(hans).printColored()
  run(updateHans(111))
  ("-" * 50).printColored(Console.RED)
  run(allPersons).printColored()
  run(allBooks).printColored()

  extension [A](a: A) {
    def printColored(c: String = Console.CYAN): A =
      a.tap(s => println(c + s.toString + Console.RESET))
  }
