package de.codecentric.wittig.scala.ducktape
import io.github.arainko.ducktape.*
import io.github.arainko.ducktape.Field.{computed, const, renamed}

object Main extends App:

  case class Person(firstName: String, lastName: String, age: Int)
  case class PersonExt(firstName: String, lastName: String, age: Int, nr: String)

  private val person    = Person("John", "Doe", 30)
  private val personExt = PersonExt("John", "Doe", 30, "a5")

  private val transformed = personExt.to[Person]

  println(transformed)

  private val builder      = person.into[PersonExt]
  private val withConstant = builder.transform(const(_.nr, "a"))
  private val withComputed = builder.transform(computed(_.nr, p => s"${p.firstName}-a5"))
  private val withRename   = builder.transform(renamed(_.nr, _.firstName))

  println(withConstant)
  println(withComputed)
  println(withRename)

  private def method(firstName: String, lastName: String, age: Int, nr: String) =
    PersonExt(firstName: String, lastName: String, age: Int, nr: String)

  private val definedViaTransformer = Transformer.defineVia[Person](method).build(Arg.const(_.nr, "ta1"))
  private val definedTransformer    = Transformer.define[Person, PersonExt].build(const(_.nr, "ta2"))

  println(definedViaTransformer.transform(person))
  println(definedTransformer.transform(person))
