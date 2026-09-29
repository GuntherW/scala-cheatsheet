package de.wittig.database.duckdb.ducklake

import java.nio.file.{Files, Path, StandardCopyOption}
import java.sql.{Connection, DriverManager}
import scala.util.Using

// DuckLake is a thin catalog layer on top of Parquet: it tracks tables, schema versions and snapshots
// in metadata, while the actual data still lives in plain Parquet files it manages underneath.
// This example imports the existing orders.parquet (from the parquet example) into a DuckLake catalog.
case class RevenueByCategory(category: String, revenue: Double, orders: Long)

@main
def main(): Unit =
  val ordersParquet = resourcePath("duckdb/orders.parquet")
  val workDir       = Files.createTempDirectory("ducklake-example")
  val metadata      = workDir.resolve("metadata.ducklake")
  val dataPath      = workDir.resolve("data_files")

  Using.resource(DriverManager.getConnection("jdbc:duckdb:")) { connection =>
    given Connection = connection

    attachDuckLake(metadata, dataPath)
    importParquet(ordersParquet)

    println("Revenue by category:")
    revenueByCategory().foreach(r => println(f"${r.category}%-12s | ${r.revenue}%8.2f | ${r.orders}%3d orders"))

    println("\nFiles DuckLake manages under data_files:")
    printTableInfo()

    println("\nSnapshots:")
    printSnapshots()
  }

// DuckDB öffnet Dateien über einen Dateisystempfad, Resources können aber im JAR ohne echten Pfad liegen.
// Deshalb wird die Resource in eine temporäre Datei kopiert, die DuckDB direkt öffnen kann.
private def resourcePath(name: String): String =
  val tempFile = Files.createTempFile("duckdb-resource-", s"-${name.replace('/', '_')}")
  tempFile.toFile.deleteOnExit()
  Using.resource(Thread.currentThread.getContextClassLoader.getResourceAsStream(name)) { in =>
    Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING)
  }
  tempFile.toString

// Hängt einen neuen DuckLake-Katalog an: die Metadaten (Tabellen, Schema-Versionen, Snapshots)
// landen in der Datei "metadata", die eigentlichen Daten als Parquet-Dateien im Verzeichnis "dataPath".
// Nach dem Attach wird mit USE auf diesen Katalog umgeschaltet, damit nachfolgende Statements ihn nutzen.
// ATTACH erlaubt keine Bind-Parameter für Pfad-Literale, daher per String-Interpolation - toAbsolutePath
// macht das Ergebnis unabhängig vom aktuellen Working Directory.
private def attachDuckLake(metadata: Path, dataPath: Path)(using connection: Connection): Unit =
  Using.resource(connection.createStatement()) { statement =>
    statement.execute(s"ATTACH 'ducklake:${metadata.toAbsolutePath}' AS my_ducklake (DATA_PATH '${dataPath.toAbsolutePath}');")
    statement.execute("USE my_ducklake;")
  }

// DuckLake does not "mount" foreign Parquet files - it takes ownership of the data it manages.
// So the existing Parquet file is imported once via CREATE TABLE AS SELECT, DuckLake then writes
// its own Parquet files under DATA_PATH and tracks them in its metadata catalog from now on.
private def importParquet(parquetPath: String)(using connection: Connection): Unit =
  Using.resource(connection.prepareStatement("CREATE TABLE orders AS SELECT * FROM read_parquet(?);")) { prepared =>
    prepared.setString(1, parquetPath)
    prepared.execute()
  }

// Berechnet den Umsatz je Kategorie - ganz normale SQL-Abfrage auf der Tabelle "orders",
// die DuckLake wie jede andere Tabelle behandelt (die Parquet-Verwaltung passiert transparent im Hintergrund).
private def revenueByCategory()(using connection: Connection): Seq[RevenueByCategory] =
  val query =
    """SELECT category, sum(quantity * unit_price) AS revenue, count(*) AS orders
      |FROM orders
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

// Zeigt, aus wie vielen physischen Parquet-Dateien eine DuckLake-Tabelle aktuell besteht
// und wie groß diese Dateien insgesamt sind - nützlich, um die Parquet-Verwaltung im Hintergrund sichtbar zu machen.
private def printTableInfo()(using connection: Connection): Unit =
  Using.Manager { use =>
    val statement = use(connection.createStatement())
    val resultSet = use(statement.executeQuery("SELECT * FROM ducklake_table_info('my_ducklake');"))
    Iterator
      .continually(resultSet)
      .takeWhile(_.next())
      .foreach(rs => println(f"table=${rs.getString("table_name")} files=${rs.getLong("file_count")} bytes=${rs.getLong("file_size_bytes")}"))
  }.get

// Listet die Versionshistorie des Katalogs auf: jede DDL-/DML-Änderung (z.B. CREATE TABLE, INSERT)
// erzeugt einen neuen Snapshot, wodurch DuckLake Time-Travel und Nachvollziehbarkeit ermöglicht.
private def printSnapshots()(using connection: Connection): Unit =
  Using.Manager { use =>
    val statement = use(connection.createStatement())
    val resultSet = use(statement.executeQuery("SELECT * FROM ducklake_snapshots('my_ducklake');"))
    Iterator
      .continually(resultSet)
      .takeWhile(_.next())
      .foreach(rs => println(f"snapshot ${rs.getLong("snapshot_id")} at ${rs.getString("snapshot_time")}"))
  }.get
