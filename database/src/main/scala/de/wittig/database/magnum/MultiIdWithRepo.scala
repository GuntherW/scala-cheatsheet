package de.wittig.database.magnum

import com.augustnagro.magnum.*
import de.wittig.database.DatabaseName.MagnumDb
import de.wittig.database.dataSource

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.util.Random
import scala.util.chaining.*

object MultiIdWithRepo extends App {

  private val xa       = Transactor(dataSource(MagnumDb), sqlLogger = SqlLogger.logSlowQueries(3.milliseconds))
  private val multRepo = MultiIdRepository()

  private val uuid = UUID.randomUUID
  private val m1   = MultId(uuid, s"name-${Random.nextString(3)}", s"multi-${Random.nextString(3)}@mail.de")
  private val m2   = MultId(uuid, s"name-${Random.nextString(3)}", s"multi-${Random.nextString(3)}@mail.de")

  transact(xa):
    multRepo.count.tap(println)
    multRepo.insert(m1)
    multRepo.insert(m2)
    multRepo.findAll.tap(println)
    multRepo.delete(m1)
    println(s"delete $m1")
    multRepo.findAll.tap(println)
}

class MultiIdRepository extends Repo[MultId, MultId, UUID]
