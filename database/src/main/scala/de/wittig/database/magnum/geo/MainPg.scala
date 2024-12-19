package de.wittig.database.magnum.geo

import com.augustnagro.magnum
import com.augustnagro.magnum.*
import com.augustnagro.magnum.pg.PgCodec.given
import de.wittig.database.DatabaseName.MagnumDb
import de.wittig.database.dataSource
import org.postgresql.geometric.*

object MainPg extends App {

  private val xa = Transactor(dataSource(MagnumDb))

  private val myGeoRepo = Repo[Geotest, Geotest, Long]

  transact(xa):
    myGeoRepo.insert(Geotest(2L, PGpoint(1, 2)))

}
