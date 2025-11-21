package de.codecentric.wittig.scala.phantomtypes.example

import scala.annotation.targetName

@main
def solution(): Unit =
  val tenString  = CodeSafe[Int]("10")
  val trueString = CodeSafe[Boolean]("true")

  tenString + tenString
  // tenString + trueString // won't compile

case class CodeSafe[A](value: String)
object CodeSafe:
  extension (code: CodeSafe[Int])
    @targetName("add")
    infix def +(that: CodeSafe[Int]): CodeSafe[Int] =
      CodeSafe(s"(${code.value} + ${that.value})")

    @targetName("multiply")
    infix def *(that: CodeSafe[Int]): CodeSafe[Int] =
      CodeSafe(s"(${code.value} * ${that.value})")

  extension (code: CodeSafe[Boolean])
    @targetName("and")
    infix def &&(that: CodeSafe[Boolean]): CodeSafe[Boolean] =
      CodeSafe(s"(${code.value} && ${that.value})")

    @targetName("or")
    infix def ||(that: CodeSafe[Boolean]): CodeSafe[Boolean] =
      CodeSafe(s"(${code.value} || ${that.value})")
