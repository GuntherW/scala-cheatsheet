import scala.util.chaining.*

import zio.schema.{DeriveSchema, Schema}
import zio.schema.syntax.*

@main
def simple(): Unit =

  final case class Person(name: String, age: Int, id: String)

  object Person:
    given schema: Schema[Person] = DeriveSchema.gen[Person]

  val p1 = Person("Alex", 31, "0123")
  val p2 = Person("Alex", 31, "124")

  val defaultPerson = Person.schema.defaultValue
  defaultPerson.tap(println)

  val patch = p1.diff(p2)

  println(patch)
  println(Person.schema)
  println(Person.schema.patch(p1, patch))
