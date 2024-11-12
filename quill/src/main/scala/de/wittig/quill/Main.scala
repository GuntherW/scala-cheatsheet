package de.wittig.quill

import scala.util.chaining.scalaUtilChainingOps

import de.wittig.quill.DBContext.*
import io.getquill.*

object Main extends App:

//  val dbContext: DBContext = H2Context
  val dbContext: DBContext = PostgresContext

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
  inline def updateHans(age: Int) = hans.update(_.age -> age)

  // clean up
  run(persons.delete)

  val p1    = Person(0, "Peter", "Pan", 88)
  val book1 = Book(0, 0, "Titel 1")
  val book2 = Book(1, 0, "Titel 2")

  // Transaction
  transaction(
    run(persons.insertValue(lift(p1))),
    run(persons.insertValue(Person(1, "Hans", "Franz", 88))),
    run(books.insertValue(lift(book1))),
    run(books.insertValue(lift(book2))),
  )

  // Update
  run(hans).printColored()
  run(updateHans(111))
  run(hans).printColored()
  ("-" * 50).printColored(Console.RED)

  run(allPersons).printColored()
  run(allBooks).printColored()
  ("-" * 50).printColored(Console.RED)

  // Applicative inner Join
  inline def peter = persons.filter(_.firstName == "Peter")
  val join         = quote {
    peter.join(books).on(_.id == _.ownerId)
  }
  ctx.translate(join).printColored(Console.GREEN)
  run(join).printColored()

  // Applicative left Join
  val joinLeft = quote {
    peter.leftJoin(books).on(_.id == _.ownerId)
  }
  ctx.translate(joinLeft).printColored(Console.GREEN)
  run(joinLeft).printColored()

  // Implicit Join
  val join2 = quote {
    for
      a <- peter
      b <- books if a.id == b.ownerId
    yield (a, b)
  }
  ctx.translate(join2).printColored(Console.GREEN)
  run(join2).printColored()

enum DBContext:
  case PostgresContext, H2Context

case class Person(id: Int, firstName: String, lastName: String, age: Int)
case class Book(id: Int, ownerId: Int, title: String)

extension [A](a: A) {
  def printColored(c: String = Console.CYAN): A = a.tap(s => println(c + s.toString + Console.RESET))
}
