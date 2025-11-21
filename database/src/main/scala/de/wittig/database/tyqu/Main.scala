package de.wittig.database.tyqu

import java.sql.DriverManager

import scala.language.{adhocExtensions, implicitConversions}

import de.wittig.database.tyqu.BookDatabase.*
import tyqu.execution.PostgreSqlQueryExecutor
import tyqu.{*, given}

@main
def main(): Unit =

  val connection = DriverManager.getConnection("jdbc:postgresql://localhost:5433/booksdb?user=postgres&password=postgres&ssl=false")
  given PostgreSqlQueryExecutor(connection)

  val results1 =
    from(Authors)
      .filter(_.birthYear > 1970)
      .sortBy(_.lastName)
      .limit(10)
      .execute()
  for author <- results1 do
    println(s"${author.firstName} ${author.lastName} (born ${author.birthYear})")

  val results2 =
    from(Authors)
      .filter(_.books.exists(_.title.contains("Scala")))
      .limit(10)
      .execute()
  for author <- results2 do
    println(s"${author.firstName} ${author.lastName} (born ${author.birthYear})")

  val currentYear = 2023
  val results4    =
    from(Authors)
      .map { a =>
        (
          (a.firstName + " " + a.lastName).as("name"),
          (currentYear - a.birthYear).as("age"),
        )
      }
      .execute()
  for author <- results4 do
    println(s"${author.name} (${author.age})")
