import Json.Person
import zio.Chunk
import zio.schema.syntax.*
import zio.schema.{DeriveSchema, Schema}
import scala.util.chaining.*

object Simple extends App:

  final case class Person(name: String, age: Int, id: String)

  object Person:
    given schema: Schema[Person] = DeriveSchema.gen[Person]

  private val p1 = Person("Alex", 31, "0123")
  private val p2 = Person("Alex", 31, "124")

  val defaultPerson = Person.schema.defaultValue
  defaultPerson.tap(println)

  private val patch = p1.diff(p2)

  println(patch)
  println(Person.schema)
  println(Person.schema.patch(p1, patch))
