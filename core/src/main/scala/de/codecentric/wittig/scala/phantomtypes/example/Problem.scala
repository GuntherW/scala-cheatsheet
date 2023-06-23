package de.codecentric.wittig.scala.phantomtypes.example

object Problem extends App:
  private val tenString  = Code("10")
  private val trueString = Code("true")

  tenString and trueString // Code((10 && true)) // unsafe

case class Code(value: String):
  infix def add(that: Code): Code =
    Code(s"($value + ${that.value})")

  infix def multiply(that: Code): Code =
    Code(s"($value * ${that.value})")

  infix def and(that: Code): Code =
    Code(s"($value && ${that.value})")

  infix def or(that: Code): Code =
    Code(s"($value || ${that.value})")
