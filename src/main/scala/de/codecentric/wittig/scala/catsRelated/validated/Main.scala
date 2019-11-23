package de.codecentric.wittig.scala.catsRelated.validated

import cats.data._
import cats.implicits._
import de.codecentric.wittig.scala.catsRelated.validated.Data.Validation._

case class Data(
    email: String,
    phone: String,
    age: Int,
    rank: Int
)

object Data {
  type Validation[A] = ValidatedNel[Error, A]

  object Validation {
    def success[T](t: T): Validation[T]     = Validated.valid(t)
    def failure[T](e: Error): Validation[T] = Validated.invalidNel(e)
  }

  private val emailRegex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r
  private val phoneRegex = """^\+(?:[0-9] ?){6,14}[0-9]$""".r

  def validateEmail(email: String): Validation[String] = email match {
    case e if emailRegex.findFirstMatchIn(e).isDefined => success(e)
    case _                                             => failure(Error.InvalidEmail)
  }

  def validatePhone(phone: String): Validation[String] = phone match {
    case p if phoneRegex.findFirstMatchIn(p).isDefined => success(p)
    case _                                             => failure(Error.InvalidPhone)
  }

  def validateAge(age: Int): Validation[Int] = age match {
    case i: Int if i > 18 => success(i)
    case _                => failure(Error.InvalidAge)
  }

  def validateRank(rank: Int): Validation[Int] = rank match {
    case i: Int if i > 0 => success(i)
    case _               => failure(Error.InvalidRank)
  }

  def validateAgeAndRank(age: Int, rank: Int): Validation[Unit] =
    if (age > rank)
      success(())
    else
      failure(Error.InvalidAgeBiggerRank)

  def validateData(d: Data): Validation[Data] = {
    val validEmail      = validateEmail(d.email)
    val validPhone      = validatePhone(d.phone)
    val validAge        = validateAge(d.age)
    val validRank       = validateRank(d.rank)
    val validAgeAndRank = validateAgeAndRank(d.age, d.rank)

    (validEmail, validPhone, validAge, validRank, validAgeAndRank).mapN {
      case (email, phone, age, rank, _) => Data(email, phone, age, rank)
    }
  }
}

object Main extends App {
  val okMail   = "a@b.de"
  val badEmail = "not valid"
  val okPhone  = "+49123456"
  val badPhone = "not valid"
  val okAge    = 56
  val badAge   = 3
  val okRank   = 2
  val badRank  = -3
  val bigRank  = 57

  import Data._
  println(validateData(Data(okMail, okPhone, okAge, okRank)))
  println(validateData(Data(badEmail, okPhone, okAge, okRank)))
  println(validateData(Data(okMail, badPhone, okAge, okRank)))
  println(validateData(Data(okMail, okPhone, okAge, bigRank)))
  println(validateData(Data(badEmail, badPhone, badAge, badRank)))
}
