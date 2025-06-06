package de.wittig.database.magnum

import com.augustnagro.magnum.*

import java.time.{LocalDate, LocalDateTime, OffsetDateTime}
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
    created: OffsetDateTime = OffsetDateTime.now,
    ts: Option[LocalDateTime] = None,
    date: Option[LocalDate] = None,
) derives DbCodec
