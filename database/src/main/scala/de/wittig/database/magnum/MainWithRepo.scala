package de.wittig.database.magnum

import java.time.{LocalDate, LocalDateTime, OffsetDateTime}
import scala.concurrent.duration.DurationInt
import com.augustnagro.magnum.*
import de.wittig.database.dataSource
import de.wittig.database.DatabaseName.MagnumDb

import java.util.UUID

object MainWithRepo extends App {

  private val xa         = Transactor(dataSource(MagnumDb), sqlLogger = SqlLogger.logSlowQueries(3.milliseconds))
  private val personRepo = PersonRepository()

  val count = connect(xa):
    personRepo.count
  println(count)

  val abc = connect(xa):
    personRepo.findByEmail("a@b.c")
  println(abc)

  val insert = connect(xa):
    personRepo.insert(Persons(UUID.randomUUID, "HannesMin", s"${UUID.randomUUID}.h.de", Color.Red, LocalDateTime.now, Some(LocalDateTime.now), Some(LocalDate.of(1970, 1, 1))))
    personRepo.insert(Persons(UUID.randomUUID, "HannesMax", s"${UUID.randomUUID}.h.de", Color.Red, LocalDateTime.now, Some(LocalDateTime.now), Some(LocalDate.of(9999, 1, 1))))

  val all = connect(xa):
    personRepo.findAll
  println(all)

  withSpecifications()

  def withSpecifications() =
    val partialEmail            = "a"
    val nameOpt: Option[String] = None
    val searchDate              = OffsetDateTime.now.minusYears(2)
    val idPosition              = 42L

    val spec = Spec[Persons]
      .where(sql"email ILIKE 'a%'")
      .where(nameOpt.map(ln => sql"name = $ln").getOrElse(sql""))
      .where(sql"created >= $searchDate")
      .limit(10)

    val users = connect(xa):
      personRepo.findAll(spec)
    users.foreach(println)
}

/* Generates:
 * def count(using DbCon): Long
 * def existsById(id: ID)(using DbCon): Boolean
 * def findAll(using DbCon): Vector[E]
 * def findAll(spec: Spec[E])(using DbCon): Vector[E]
 * def findById(id: ID)(using DbCon): Option[E]
 * def findAllById(ids: Iterable[ID])(using DbCon): Vector[E]
 *
 * def delete(entity: E)(using DbCon): Unit
 * def deleteById(id: ID)(using DbCon): Unit
 * def truncate()(using DbCon): Unit
 * def deleteAll(entities: Iterable[E])(using DbCon): BatchUpdateResult
 * def deleteAllById(ids: Iterable[ID])(using DbCon): BatchUpdateResult
 * def insert(entityCreator: EC)(using DbCon): Unit
 * def insertAll(entityCreators: Iterable[EC])(using DbCon): Unit
 * def insertReturning(entityCreator: EC)(using DbCon): E
 * def insertAllReturning(entityCreators: Iterable[EC])(using DbCon): Vector[E]
 * def update(entity: E)(using DbCon): Unit
 * def updateAll(entities: Iterable[E])(using DbCon): BatchUpdateResult
 */
class PersonRepository extends Repo[Persons, Persons, Long]:

  def findByEmail(email: String)(using DbCon) =
    sql"SELECT * FROM person WHERE email=$email".query[Persons].run()
