package de.wittig.quill

import io.getquill.*

object Main extends App:
  case class Person(firstName: String, lastName: String, age: Int)

  val ctx = new PostgresJdbcContext(SnakeCase, "ctxpg")
  import ctx.*

  inline def persons = query[Person]
  inline def all     = quote(persons)
  inline def hans    = quote(persons.filter(_.firstName == "Hans"))

  inline def updateHans() = quote(persons.filter(_.firstName == "Hans").update(_.age -> 123))

  pc(run(hans).toString)
  run(updateHans())
  println("-" * 50)
  pc(run(all).toString)

  private def pc(msg: String): Unit = println(Console.CYAN + msg + Console.RESET)
