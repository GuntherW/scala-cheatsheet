import zio.Console.printLine
import zio.json.ast.Json
import zio.schema.codec.ProtobufCodec.*
import zio.schema.{DeriveSchema, Schema}
import zio.stream.ZStream
import zio.{Chunk, ExitCode, URIO}
import zio.schema.syntax.*

object Main extends App:

  final case class Person(name: String, age: Int, id: String)

  object Person:
    implicit val schema: Schema[Person] = DeriveSchema.gen[Person]

  private val p1    = Person("Alex", 31, "0123")
  private val p2    = Person("Alex", 31, "124")
  private val patch = p1.diff(p2)

  println("Hallo Welt")
  println(patch)
  println(Person.schema)
  println(Person.schema.patch(p1, patch))
