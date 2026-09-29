package de.wittig.database.duckdb.csv

import java.nio.file.{Files, StandardCopyOption}
import java.sql.{Connection, DriverManager}
import scala.util.Using

// DuckDB can analyze CSV files directly with SQL, without loading them into a table first.
case class RevenueByCategory(category: String, revenue: Double, orders: Long)
case class RevenueByCountry(country: String, revenue: Double, orders: Long)

@main
def main(): Unit =
  val ordersCsv = resourcePath("duckdb/orders.csv")

  Using.resource(DriverManager.getConnection("jdbc:duckdb:")) { connection =>
    given Connection = connection

    println("Revenue by category:")
    revenueByCategory(ordersCsv).foreach(r => println(f"${r.category}%-12s | ${r.revenue}%8.2f | ${r.orders}%3d orders"))

    println("\nRevenue by country:")
    revenueByCountry(ordersCsv).foreach(r => println(f"${r.country}%-10s | ${r.revenue}%8.2f | ${r.orders}%3d orders"))
  }

// DuckDB opens files by filesystem path, but resources may live inside a JAR without a real path.
// So the resource is copied to a temp file that DuckDB can open directly.
private def resourcePath(name: String): String =
  val tempFile = Files.createTempFile("duckdb-resource-", s"-${name.replace('/', '_')}")
  tempFile.toFile.deleteOnExit()
  Using.resource(Thread.currentThread.getContextClassLoader.getResourceAsStream(name)) { in =>
    Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING)
  }
  tempFile.toString

private def revenueByCategory(csvPath: String)(using connection: Connection): Seq[RevenueByCategory] =
  val query =
    """SELECT category, sum(quantity * unit_price) AS revenue, count(*) AS orders
      |FROM read_csv_auto(?)
      |WHERE status != 'cancelled'
      |GROUP BY category
      |ORDER BY revenue DESC;""".stripMargin
  Using.Manager { use =>
    val prepared  = use(connection.prepareStatement(query))
    prepared.setString(1, csvPath)
    val resultSet = use(prepared.executeQuery())
    Iterator
      .continually(resultSet)
      .takeWhile(_.next())
      .map(rs => RevenueByCategory(rs.getString("category"), rs.getDouble("revenue"), rs.getLong("orders")))
      .toSeq
  }.get

private def revenueByCountry(csvPath: String)(using connection: Connection): Seq[RevenueByCountry] =
  val query =
    """SELECT country, sum(quantity * unit_price) AS revenue, count(*) AS orders
      |FROM read_csv_auto(?)
      |WHERE status != 'cancelled'
      |GROUP BY country
      |ORDER BY revenue DESC;""".stripMargin
  Using.Manager { use =>
    val prepared  = use(connection.prepareStatement(query))
    prepared.setString(1, csvPath)
    val resultSet = use(prepared.executeQuery())
    Iterator
      .continually(resultSet)
      .takeWhile(_.next())
      .map(rs => RevenueByCountry(rs.getString("country"), rs.getDouble("revenue"), rs.getLong("orders")))
      .toSeq
  }.get
