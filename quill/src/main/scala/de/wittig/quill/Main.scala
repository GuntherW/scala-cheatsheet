package de.wittig.quill

import io.getquill.*
import io.getquill.context.jdbc.JdbcContext

object Main extends App:
  case class Person(firstName: String, lastName: String, age: Int)

//  val ctx = new PostgresJdbcContext(SnakeCase, "ctxpg")
  val ctx = new H2JdbcContext(SnakeCase, "ctxh2")
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
      println(c + a.toString + Console.RESET)
      a
  }
