package de.wittig.database.scalasql
import scalasql._, H2Dialect._
import org.h2.jdbcx.JdbcDataSource
import scala.util.chaining.*

object H2Example extends App:

  // Define your table model classes
  case class City[T[_]](
      id: T[Int],
      name: T[String],
      countryCode: T[String],
      district: T[String],
      population: T[Long]
  )

  object City extends Table[City]

  // Connect to your database (example uses in-memory sqlite, org.xerial:sqlite-jdbc:3.43.0.0)
  val dataSource = JdbcDataSource()
    .tap(_.setURL("jdbc:h2:/tmp/h2"))
    .tap(_.setUser("sa"))
    .tap(_.setPassword(""))

  lazy val dbClient = new scalasql.DbClient.DataSource(
    dataSource,
    config = new scalasql.Config {
      override def nameMapper(v: String)                        = v.toLowerCase() // Override default snake_case mapper
      override def logSql(sql: String, file: String, line: Int) = println(s"$file:$line $sql")
    }
  )

  dbClient.transaction { db =>

    // Initialize database table schema and data
    db.updateRaw(os.read(os.Path("database/src/main/resources/sql/world-schema.sql", os.pwd)))
    // db.updateRaw(os.read(os.Path("database/src/main/resources/sql/world-data.sql", os.pwd)))

    val citiesPop = db.run(City.select.filter(_.countryCode === "CHN").map(_.population).sum)
    println(citiesPop) // 175953614

    val fewLargestCities = db.run(
      City.select
        .sortBy(_.population).desc
        .drop(5).take(3)
        .map(c => (c.name, c.population))
    )
    println(fewLargestCities) // Seq((Karachi, 9269265), (Istanbul, 8787958), (Ciudad de México, 8591309))
  }
