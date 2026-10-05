package de.wittig.database.magnum

import com.augustnagro.magnum.*
import de.wittig.database.DatabaseName.MagnumDb
import de.wittig.database.dataSource

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.util.Random
import scala.util.chaining.*

@main
def multiIdWithRepo(): Unit =

  val xa       = Transactor(dataSource(MagnumDb), sqlLogger = SqlLogger.logSlowQueries(3.milliseconds))
  val multRepo = MultiIdRepository()

  val uuid = UUID.randomUUID
  val m1   = MultId(uuid, s"m1", s"m1@mail.de")
  val m2   = MultId(uuid, s"m2", s"m2@mail.de")

  transact(xa):
    multRepo.truncate()
    require(multRepo.count == 0)
    multRepo.insertAll(List(m1, m2))
    require(multRepo.count == 2)
    multRepo.findAll.tap(println)
    multRepo.delete(m1).tap(_ => println(s"delete $m1"))
    multRepo.findAll.tap(println)
    require(multRepo.count == 1)

class MultiIdRepository extends Repo[MultId, MultId, (UUID, String)]

@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
case class MultId(
    @Id id: UUID,
    @Id name: String,
    email: String,
) derives DbCodec
