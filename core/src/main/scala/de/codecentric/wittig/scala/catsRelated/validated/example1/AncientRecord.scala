package de.codecentric.wittig.scala.catsRelated.validated.example1

import cats.effect.IO

case class AncientRecord(
    age: String,
    name: String,
    rate: String
)

case class Citizen(
    age: Int,
    name: String,
    rate: Double
)

object Parsing {

  def parseAge(age: String): IO[Int]      = ???
  def parseName(name: String): IO[String] = ???
  def parseRate(rate: String): IO[Double] = ???

  def parseRecord(rec: AncientRecord): IO[Citizen] =
    for {
      age  <- parseAge(rec.age)
      name <- parseName(rec.name)
      rate <- parseRate(rec.rate)
    } yield Citizen(age, name, rate)
}
