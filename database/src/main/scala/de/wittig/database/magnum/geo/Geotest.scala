package de.wittig.database.magnum.geo

import com.augustnagro.magnum.pg.PgCodec.given
import com.augustnagro.magnum.{DbCodec, Id, PostgresDbType, Table}
import org.postgresql.geometric.PGpoint

@Table(PostgresDbType)
case class Geotest(@Id id: Long, pnts: PGpoint) derives DbCodec
