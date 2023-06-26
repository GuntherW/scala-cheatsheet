package de.codecentric.wittig.scala.phantomtypes.example

import scala.annotation.targetName

object Problem extends App:
  private val tenString  = Code("10")
  private val trueString = Code("true")

  tenString && trueString // Code((10 && true)) // unsafe

case class Code(value: String):
  @targetName("add")
  infix def +(that: Code): Code =
    Code(s"($value + ${that.value})")

  @targetName("multiply")
  infix def *(that: Code): Code =
    Code(s"($value * ${that.value})")

  @targetName("and")
  infix def &&(that: Code): Code =
    Code(s"($value && ${that.value})")

  @targetName("or")
  infix def ||(that: Code): Code =
    Code(s"($value || ${that.value})")
