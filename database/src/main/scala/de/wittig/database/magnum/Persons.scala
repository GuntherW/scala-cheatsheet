package de.wittig.database.magnum

import com.augustnagro.magnum.*

import java.sql.{Date, Timestamp}
import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
enum Color derives DbCodec:
  case Red, Green, Blue, BlueOrange

@SqlName("person") // Falls sich Tabellenname unterscheidet
@Table(PostgresDbType)
case class Persons(
    @Id id: UUID,
    name: String,
    email: String,
    color: Color,
    created: LocalDateTime = LocalDateTime.now,
    ts: Option[LocalDateTime] = None,
    date: Option[LocalDate] = None,
) derives DbCodec

object Persons:

  // TODO: Nullabfrage in v2.0.0 nicht mehr nötig
  given DbCodec[LocalDateTime] = DbCodec.SqlTimestampCodec.biMap(ts => if ts == null then null else ts.toLocalDateTime, Timestamp.valueOf)

  // TODO: Nullabfrage in v2.0.0 nicht mehr nötig, Außerdem ist das "-infinity", "infinity" hier nicht gelöst. Wartet auf Standardcodec aus der Magnumlib.
  given DbCodec[LocalDate] = DbCodec.SqlDateCodec.biMap(ts => if ts == null then null else ts.toLocalDate, Date.valueOf)
