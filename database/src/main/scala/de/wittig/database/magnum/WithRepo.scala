package de.wittig.database.magnum

import com.augustnagro.magnum.*
import de.wittig.database.DatabaseName.MagnumDb
import de.wittig.database.dataSource

import java.time.{LocalDate, LocalDateTime, OffsetDateTime}
import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.util.chaining.*

object MainWithRepo extends App {

  private val xa         = Transactor(dataSource(MagnumDb), sqlLogger = SqlLogger.logSlowQueries(3.milliseconds))
  private val personRepo = PersonRepository()

  private val p1 = Persons(UUID.randomUUID, "HannesMin", s"${UUID.randomUUID}.h.de", Color.Red, LocalDateTime.now, Some(LocalDateTime.now), Some(LocalDate.of(1970, 1, 1)))
  private val p2 = Persons(UUID.randomUUID, "HannesMax", s"${UUID.randomUUID}.h.de", Color.Red, LocalDateTime.now, Some(LocalDateTime.now), Some(LocalDate.of(9999, 1, 1)))

  val count = transact(xa):
    personRepo.count.tap(println)
    personRepo.findByEmail("a@b.c").tap(println)
    personRepo.insert(p1)
    personRepo.insert(p2)
    personRepo.findAll.tap(println)

  withSpecifications()

  val delete = connect(xa):
    personRepo.delete(p1)
    personRepo.delete(p2)

  val all2 = connect(xa):
    println("After deletion")
    personRepo.findAll.tap(println)
    personRepo.count.tap(println)

  def withSpecifications() =
    val partialEmail            = "a"
    val nameOpt: Option[String] = None
    val searchDate              = OffsetDateTime.now.minusYears(2)

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
class PersonRepository extends Repo[Persons, Persons, UUID]:

  def findByEmail(email: String)(using DbCon) =
    sql"SELECT * FROM person WHERE email=$email".query[Persons].run()
