package de.codecentric.wittig.scala.typeerasure

import scala.reflect.{classTag, ClassTag}

case class Teil[T](inhalt: T)

object ClassTagExample extends App:

  def usingClassTag[T: ClassTag](l: Teil[T]): Unit =
    println(s"mit ClassTag: ${l.inhalt} ${classTag[T].runtimeClass}")

  usingClassTag(Teil("Teil"))
  usingClassTag(Teil(2))
  usingClassTag(Teil(BigDecimal(2)))
