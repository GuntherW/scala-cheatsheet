package de.wittig.database.magnum

import com.augustnagro.magnum
import com.augustnagro.magnum.*
import de.wittig.database.DatabaseName.MagnumDb
import de.wittig.database.dataSource

import java.sql.Connection
import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.util.Random

@main
def simple(): Unit =

  // Transactor lets you customize the transaction (or connection) behavior.
  val xa = Transactor(
    dataSource(MagnumDb),
    sqlLogger = SqlLogger.logSlowQueries(5.milliseconds),
    connectionConfig = con => con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ)
  )

  // Simple Query
  val persons: Vector[Persons] =
    connect(xa):
      sql"SELECT * FROM person".query[Persons].run()
  persons.foreach(println)

  // Transaction
  val randomString = Random.nextString(10)
  transact(xa):
    sql"UPDATE person SET name = $randomString WHERE email = 'a@b.c'".update.run()
//    throw new RuntimeException("Boom")

  // Returning Id
  val updateId: Vector[UUID] =
    connect(xa):
      sql"""UPDATE person
         SET name = $randomString
         WHERE email = 'a@b.c'
         RETURNING id
         """.returning[UUID].run()
  updateId.foreach(println)

  // Batch update
  connect(xa):
    val persons: Iterable[Persons]      = List(Persons(UUID.randomUUID, "Gunner", "a@b.c", Color.BlueOrange))
    val updateResult: BatchUpdateResult =
      batchUpdate(persons): user =>
        sql"UPDATE person SET name = $randomString, color = ${user.color} WHERE email = 'a@b.c'".update

    updateResult match
      case magnum.BatchUpdateResult.Success(rowsUpdated) => println(s"Rows updated: $rowsUpdated")
      case magnum.BatchUpdateResult.SuccessNoInfo        => println("No rows updated")
