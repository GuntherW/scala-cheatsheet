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
  val m1   = MultId(uuid, s"m1-${Random.nextString(3)}", s"m1-${Random.nextString(3)}@mail.de")
  val m2   = MultId(uuid, s"m2-${Random.nextString(3)}", s"m2-${Random.nextString(3)}@mail.de")

  transact(xa):
    multRepo.count.tap(println)
    multRepo.insertAll(List(m1, m2))
    multRepo.findAll.tap(println)
    multRepo.delete(m1).tap(_ => println(s"delete $m1"))
    multRepo.findAll.tap(println) // Hier sollte jetzt noch m2 in der db sein.

class MultiIdRepository extends Repo[MultId, MultId, UUID]

@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
case class MultId(
    @Id id: UUID,
    @Id name: String,
    email: String,
) derives DbCodec
