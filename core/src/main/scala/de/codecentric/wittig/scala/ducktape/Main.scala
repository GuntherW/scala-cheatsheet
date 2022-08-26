package de.codecentric.wittig.scala.ducktape
import io.github.arainko.ducktape.*
import io.github.arainko.ducktape.Field.{computed, const, renamed}
object Main extends App:

  final case class Person(firstName: String, lastName: String, age: Int)
  final case class PersonExt(firstName: String, lastName: String, age: Int, nr: String)

  val person    = Person("John", "Doe", 30)
  val personExt = PersonExt("John", "Doe", 30, "a5")

  val transformed = personExt.to[Person]

  println(transformed)

  val builder      = person.into[PersonExt]
  val withConstant = builder.transform(const(_.nr, "a"))
  val withComputed = builder.transform(computed(_.nr, p => s"${p.firstName}-a5"))
  val withRename   = builder.transform(renamed(_.nr, _.firstName))

  println(withConstant)
  println(withComputed)
  println(withRename)

  def method(firstName: String, lastName: String, age: Int, nr: String) = PersonExt(firstName: String, lastName: String, age: Int, nr: String)

  val definedViaTransformer = Transformer.defineVia[Person](method).build(Arg.const(_.nr, "ta1"))
  val definedTransformer    = Transformer.define[Person, PersonExt].build(const(_.nr, "ta2"))

  println(definedViaTransformer.transform(person))
  println(definedTransformer.transform(person))
