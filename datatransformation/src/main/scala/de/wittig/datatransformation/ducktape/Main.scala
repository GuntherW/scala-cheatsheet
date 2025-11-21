package de.wittig.datatransformation.ducktape

import io.github.arainko.ducktape.{to, *}

@main
def main(): Unit =

  case class Person(firstName: String, lastName: String, age: Int)
  case class PersonExt(firstName: String, lastName: String, age: Int, nr: String)

  val person    = Person("John", "Doe", 30)
  val personExt = PersonExt("John", "DoeExt", 30, "a5")

  val transformed = personExt.to[Person]

  println(transformed)

  val builder      = person.into[PersonExt]
  val withConstant = builder.transform(Field.const(_.nr, "a"))
  val withComputed = builder.transform(Field.computed(_.nr, p => s"${p.firstName}-a5"))
  val withRename   = builder.transform(Field.renamed(_.nr, _.firstName))

  println(withConstant)
  println(withComputed)
  println(withRename)

  val definedTransformer = Transformer.define[Person, PersonExt].build(Field.const(_.nr, "ta2"))

  println(definedTransformer.transform(person))
