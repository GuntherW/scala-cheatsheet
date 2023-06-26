package de.wittig.quill

import io.getquill.*
import io.getquill.context.jdbc.JdbcContext
import DBContext.*

import scala.util.chaining.scalaUtilChainingOps

enum DBContext {
  case PostgresContext, H2Context
}
case class Person(firstName: String, lastName: String, age: Int)

object Main extends App:

  val dbContext: DBContext = H2Context

  private val ctx = dbContext match {
    case PostgresContext => new PostgresJdbcContext(SnakeCase, "ctxpg")
    case H2Context       => new H2JdbcContext(SnakeCase, "ctxh2")
  }
  import ctx.*

  inline def persons              = query[Person]
  inline def all                  = quote(persons)
  inline def hans                 = quote(persons.filter(_.firstName == "Hans"))
  inline def updateHans(age: Int) = quote(persons.filter(_.firstName == "Hans").update(_.age -> age))

  transaction(
    run(persons.insertValue(Person("Peter", "Pan", 88))).printColored(Console.YELLOW),
    run(persons.insertValue(Person("Hans", "Franz", 88))).printColored(Console.YELLOW),
  )
  run(hans).printColored()
  run(updateHans(111))
  ("-" * 50).printColored(Console.RED)
  run(all).printColored()

  extension [A](a: A) {
    def printColored(c: String = Console.CYAN): A =
      a.tap(s => println(c + s.toString + Console.RESET))
  }
