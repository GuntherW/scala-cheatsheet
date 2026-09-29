package de.wittig.database.duckdb.parquet

import java.nio.file.{Files, StandardCopyOption}
import java.sql.{Connection, DriverManager}
import scala.util.Using

// Parquet is a columnar format, DuckDB's native strength: read_parquet scans only the columns it needs.
case class RevenueByCategory(category: String, revenue: Double, orders: Long)
case class RevenueByCountry(country: String, revenue: Double, orders: Long)

@main
def main(): Unit =
  val ordersParquet = resourcePath("duckdb/orders.parquet")

  Using.resource(DriverManager.getConnection("jdbc:duckdb:")) { connection =>
    given Connection = connection

    println("Revenue by category:")
    revenueByCategory(ordersParquet).foreach(r => println(f"${r.category}%-12s | ${r.revenue}%8.2f | ${r.orders}%3d orders"))

    println("\nRevenue by country:")
    revenueByCountry(ordersParquet).foreach(r => println(f"${r.country}%-10s | ${r.revenue}%8.2f | ${r.orders}%3d orders"))
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

private def revenueByCategory(parquetPath: String)(using connection: Connection): Seq[RevenueByCategory] =
  val query =
    s"""SELECT category, sum(quantity * unit_price) AS revenue, count(*) AS orders
       |FROM read_parquet('$parquetPath')
       |WHERE status != 'cancelled'
       |GROUP BY category
       |ORDER BY revenue DESC;""".stripMargin
  Using.Manager { use =>
    val statement = use(connection.createStatement())
    val resultSet = use(statement.executeQuery(query))
    Iterator
      .continually(resultSet)
      .takeWhile(_.next())
      .map(rs => RevenueByCategory(rs.getString("category"), rs.getDouble("revenue"), rs.getLong("orders")))
      .toSeq
  }.get

private def revenueByCountry(parquetPath: String)(using connection: Connection): Seq[RevenueByCountry] =
  val query =
    s"""SELECT country, sum(quantity * unit_price) AS revenue, count(*) AS orders
       |FROM read_parquet('$parquetPath')
       |WHERE status != 'cancelled'
       |GROUP BY country
       |ORDER BY revenue DESC;""".stripMargin
  Using.Manager { use =>
    val statement = use(connection.createStatement())
    val resultSet = use(statement.executeQuery(query))
    Iterator
      .continually(resultSet)
      .takeWhile(_.next())
      .map(rs => RevenueByCountry(rs.getString("country"), rs.getDouble("revenue"), rs.getLong("orders")))
      .toSeq
  }.get
