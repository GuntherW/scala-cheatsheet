package de.wittig.database.magnum

import com.augustnagro.magnum.*
import de.wittig.database.DatabaseName.MagnumDb
import de.wittig.database.dataSource

import java.util.UUID
import scala.concurrent.duration.DurationInt

object Seek extends App {

  private val xa         = Transactor(dataSource(MagnumDb), sqlLogger = SqlLogger.logSlowQueries(3.milliseconds))
  private val personRepo = PersonRepository()
  private val persons    = List.tabulate(30)(i => Persons(UUID.randomUUID, s"Gunner$i", s"a$i@b.c", Color.BlueOrange))

  // Setup
  connect(xa):
    val updateResult: BatchUpdateResult =
      batchUpdate(persons): user =>
        sql"insert into person values ($user)".update

    updateResult match
      case BatchUpdateResult.Success(rowsUpdated) => println(s"Rows updated: $rowsUpdated")
      case BatchUpdateResult.SuccessNoInfo        => println("No rows updated")

  private val idPosition2 = persons(19)
  println(s"id position : ${idPosition2.name}, ${idPosition2.id}")
  println("#" * 100)

  connect(xa):
    personRepo.findAll(
      Spec[Persons]
        .seek("name", SeekDir.Gt, idPosition2.name, SortOrder.Asc)
        .limit(5)
    ).foreach(println)

  println("#" * 100)

  connect(xa):
    personRepo.findAll(
      Spec[Persons]
        .seek("name", SeekDir.Lt, idPosition2.name, SortOrder.Desc)
        .limit(5)
    ).foreach(println)

  println("#" * 100)

  connect(xa):
    personRepo.findAll(
      Spec[Persons]
        .seek("name", SeekDir.Gt, idPosition2.name, SortOrder.Asc)
        // Falls name nicht unique ist, dann muß noch ein weiteres Feld benutzt werden, damit bei der Paginierung nicht Entities verschluckt werden.
        .seek("id", SeekDir.Gt, idPosition2.id, SortOrder.Asc)
        .limit(5)
    ).foreach(println)

  // clean up
  println("#" * 100)
  connect(xa):
    println(s"before deletion count: ${personRepo.count}")
    sql"""DELETE from person WHERE name like 'Gunner%'""".update.run()
    println(s"after deletion count: ${personRepo.count}")

}
