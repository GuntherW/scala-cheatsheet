package de.wittig.database.magnum

import java.time.{LocalDateTime, OffsetDateTime}
import java.util.UUID
import com.augustnagro.magnum.*

import java.sql.Timestamp

@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
enum Color derives DbCodec:
  case Red, Green, Blue, BlueOrange

@Table(PostgresDbType)
case class Person(
    @Id id: UUID,
    name: String,
    email: String,
    color: Color,
    created: LocalDateTime = LocalDateTime.now,
    date: Option[LocalDateTime] = None
) derives DbCodec

object Person:
  given DbCodec[LocalDateTime] = DbCodec.SqlTimestampCodec.biMap(ts => if ts == null then null else ts.toLocalDateTime, Timestamp.valueOf) // TODO: Nullabfrage in v2.0.0 nicht mehr nötig
