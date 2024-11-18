package de.wittig.database.magnum

import java.sql.Connection

import scala.concurrent.duration.DurationInt

import com.augustnagro.magnum
import com.augustnagro.magnum.*
import de.wittig.database.hikariDataSource
import de.wittig.database.DatabaseName.MagnumDb

object MainSimpleHikari extends App {

  // Transactor lets you customize the transaction (or connection) behavior.
  val xa = Transactor(
    hikariDataSource(MagnumDb),
    sqlLogger = SqlLogger.logSlowQueries(5.milliseconds),
    connectionConfig = con => con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ)
  )

  // Simple Query
  val persons: Vector[Person] = connect(xa):
    sql"SELECT * FROM person".query[Person].run()
  persons.foreach(println)

}
