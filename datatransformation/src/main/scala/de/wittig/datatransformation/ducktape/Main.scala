package de.wittig.datatransformation.ducktape

import io.github.arainko.ducktape.{to, *}

object Main extends App:

  private case class Person(firstName: String, lastName: String, age: Int)
  private case class PersonExt(firstName: String, lastName: String, age: Int, nr: String)

  private val person    = Person("John", "Doe", 30)
  private val personExt = PersonExt("John", "DoeExt", 30, "a5")

  private val transformed = personExt.to[Person]

  println(transformed)

  private val builder      = person.into[PersonExt]
  private val withConstant = builder.transform(Field.const(_.nr, "a"))
  private val withComputed = builder.transform(Field.computed(_.nr, p => s"${p.firstName}-a5"))
  private val withRename   = builder.transform(Field.renamed(_.nr, _.firstName))

  println(withConstant)
  println(withComputed)
  println(withRename)

  private val definedTransformer = Transformer.define[Person, PersonExt].build(Field.const(_.nr, "ta2"))

  println(definedTransformer.transform(person))
