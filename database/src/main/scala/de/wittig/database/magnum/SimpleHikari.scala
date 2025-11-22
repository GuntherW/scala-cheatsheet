package de.wittig.database.magnum

import com.augustnagro.magnum.*
import de.wittig.database.DatabaseName.MagnumDb
import de.wittig.database.hikariDataSource

import java.sql.Connection
import scala.concurrent.duration.DurationInt

@main
def simpleHikari(): Unit =

  // Transactor lets you customize the transaction (or connection) behavior.
  val xa = Transactor(
    hikariDataSource(MagnumDb),
    sqlLogger = SqlLogger.logSlowQueries(5.milliseconds),
    connectionConfig = con => con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ)
  )

  // Simple Query
  val persons: Vector[Persons] = connect(xa):
    sql"SELECT * FROM person".query[Persons].run()
  persons.foreach(println)
