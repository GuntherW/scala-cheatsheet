package de.wittig.database.magnum.geo

import com.augustnagro.magnum.{DbCodec, Id, PostgresDbType, SqlNameMapper, Table}
import org.postgresql.geometric.PGpoint
import com.augustnagro.magnum.pg.PgCodec.*
import com.augustnagro.magnum.pg.PgCodec.given

@Table(PostgresDbType)
case class Geotest(@Id id: Long, pnts: PGpoint) derives DbCodec
