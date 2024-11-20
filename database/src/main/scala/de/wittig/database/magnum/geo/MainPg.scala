package de.wittig.database.magnum.geo

import java.sql.Connection

import scala.concurrent.duration.DurationInt

import com.augustnagro.magnum
import com.augustnagro.magnum.*
import com.augustnagro.magnum.pg.PgCodec.given
import de.wittig.database.dataSource
import de.wittig.database.DatabaseName.MagnumDb
import org.postgresql.geometric.*

object MainPg extends App {

  private val xa = Transactor(dataSource(MagnumDb))

  private val myGeoRepo = Repo[Geotest, Geotest, Long]

  transact(xa):
    myGeoRepo.insert(Geotest(2L, PGpoint(1, 2)))

}
