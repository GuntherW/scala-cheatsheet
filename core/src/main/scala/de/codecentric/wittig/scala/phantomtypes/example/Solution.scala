package de.codecentric.wittig.scala.phantomtypes.example

object Solution extends App:
  private val tenString  = CodeSafe[Int]("10")
  private val trueString = CodeSafe[Boolean]("true")

  tenString add tenString
  // tenString and trueString // won't compile

case class CodeSafe[A](value: String)
object CodeSafe:
  extension (intCode: CodeSafe[Int])
    infix def add(that: CodeSafe[Int]): CodeSafe[Int] =
      CodeSafe(s"(${intCode.value} + ${that.value})")

    infix def multiply(that: CodeSafe[Int]): CodeSafe[Int] =
      CodeSafe(s"(${intCode.value} * ${that.value})")

  extension (boolCode: CodeSafe[Boolean])
    infix def and(that: CodeSafe[Boolean]): CodeSafe[Boolean] =
      CodeSafe(s"(${boolCode.value} && ${that.value})")

    infix def or(that: CodeSafe[Boolean]): CodeSafe[Boolean] =
      CodeSafe(s"(${boolCode.value} || ${that.value})")
