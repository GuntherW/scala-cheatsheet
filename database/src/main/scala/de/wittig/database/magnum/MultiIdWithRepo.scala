package de.wittig.database.magnum

import com.augustnagro.magnum.*
import de.wittig.database.DatabaseName.MagnumDb
import de.wittig.database.dataSource

import java.util.UUID
import scala.concurrent.duration.DurationInt

object MultiIdWithRepo extends App {

  private val xa       = Transactor(dataSource(MagnumDb), sqlLogger = SqlLogger.logSlowQueries(3.milliseconds))
  private val multRepo = MultiIdRepository()

  private val m1 = MultId(UUID.randomUUID, "name", "e@mail.de")

  val count = connect(xa):
    multRepo.count
  println(count)

  val abc = connect(xa):
    multRepo.findByEmail("a@b.c")
  println(abc)

  val insert = connect(xa):
    multRepo.insert(m1)

  val all = connect(xa):
    multRepo.findAll
  println(all)

  // does not work
  val delete = connect(xa):
    multRepo.delete(m1)

  val all2 = connect(xa):
    multRepo.findAll
  println("after delete:")
  println(all2)

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
class MultiIdRepository extends Repo[MultId, MultId, Long]:

  def findByEmail(email: String)(using DbCon) =
    sql"SELECT * FROM person WHERE email=$email".query[Persons].run()
