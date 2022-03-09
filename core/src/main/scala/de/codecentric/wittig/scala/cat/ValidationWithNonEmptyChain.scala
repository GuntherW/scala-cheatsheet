package de.codecentric.wittig.scala.cat

import cats.*
import cats.data.ValidatedNec
import cats.implicits.*
import cats.implicits.catsSyntaxValidatedIdBinCompat0

case class User(name: String, password: String)

object ValidationWithNonEmptyChain extends App:

  type ValidationResult[A] = ValidatedNec[String, A]

  private def validateUserName(userName: String): ValidationResult[String] =
    if userName.matches("^[a-zA-Z0-9]+$") then userName.validNec
    else "wrong username".invalidNec

  private def isLowerCase(s: String): ValidationResult[String] =
    if s == s.toLowerCase then s.validNec
    else "not lowercase".invalidNec

  private def lowerUserName(userName: String) =
    validateUserName(userName).andThen(isLowerCase)

  private def validatePassword(password: String): ValidationResult[String] =
    if password.matches("(?=^.{10,}$)((?=.*\\d)|(?=.*\\W+))(?![.\\n])(?=.*[A-Z])(?=.*[a-z]).*$") then password.validNec
    else "wrong password".invalidNec

  val validUser = (
    lowerUserName("username"),
    validatePassword("passW0rd123")
  ).mapN(User.apply)

  val invalidUser = (
    validateUserName("%%%"),
    validatePassword("pass")
  ).mapN(User.apply)

  println(validUser)
  println(invalidUser)
