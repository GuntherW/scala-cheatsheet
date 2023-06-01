package de.codecentric.wittig.scala.monocle
import monocle.syntax.all.*

import java.time.YearMonth
import scala.util.chaining.*

object Main extends App:

  val user = User(
    "Anna",
    40,
    Address(12, "high street"),
    Some(Hobby("Singen")),
    List(
      DebitCard("4568 5794 3109 3087", YearMonth.of(2022, 4), 361),
      DebitCard("5566 2337 3022 2470", YearMonth.of(2024, 8), 990)
    ),
    Map(
      "Computer Science" -> Department(
        45,
        List(Lecturer("john", "doe", 10), Lecturer("robert", "johnson", 16))
      ),
      "History"          -> Department(
        30,
        List(Lecturer("arnold", "stones", 20))
      )
    )
  )

  user
    .focus(_.name)
    .replace("Bob")
    .tap(println)

  user
    .focus(_.address)
    .modify(add => add.copy(streetNumber = add.streetNumber + 1, streetName = add.streetName.toUpperCase))
    .tap(println)

  user
    .focus(_.address.streetNumber)
    .modify(_ + 1)
    .tap(println)

  // Optional
  user
    .focus(_.hobby)
    .replace(Some(Hobby("Tanzen")))
    .tap(println)

  user
    .focus(_.hobby.some.name)
    .modify(_.toUpperCase)
    .tap(println)

  // List
  user
    .focus(_.debitCards.index(0).expirationDate)
    .replace(YearMonth.of(2026, 2))
    .tap(println)

  // Map
  user
    .focus(_.departments.at("History")) // at kann auch inserten oder löschen
    .replace(None)
    .tap(println)

  user
    .focus(_.departments.index("History").budget) // index kann nur existierende Werte updaten
    .replace(4)
    .tap(println)

  // List
  user
    .focus(_.departments.index("History").lecturers)
    .replace(List(Lecturer("ll", "", 5)))
    .tap(println)

  user
    .focus(_.departments.index("Computer Science").lecturers.index(0).firstName)
    .modify(_.toUpperCase)
    .tap(println)

  user
    .focus(_.departments.index("Computer Science").lecturers.each.firstName)
    .modify(_.toUpperCase)
    .tap(println)

  user
    .focus(_.departments.index("Computer Science").lecturers.each)
    .find(_.firstName.startsWith("j")) // getAll, headOption, all
    .tap(println)

case class User(
    name: String,
    age: Int,
    address: Address,
    hobby: Option[Hobby],
    debitCards: List[DebitCard] = Nil,
    departments: Map[String, Department]
)
case class Address(streetNumber: Int, streetName: String)
case class Hobby(name: String)
case class DebitCard(cardNumber: String, expirationDate: YearMonth, securityCode: Int)
case class Lecturer(firstName: String, lastName: String, salary: Int)
case class Department(budget: Int, lecturers: List[Lecturer])
