package de.wittig.database.magnum

import java.time.OffsetDateTime
import java.util.UUID

import com.augustnagro.magnum.*

@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
enum Color derives DbCodec:
  case Red, Green, Blue, BlueOrange

@Table(PostgresDbType)
case class Person(
    @Id id: UUID,
    name: String,
    email: String,
    color: Color,
    created: OffsetDateTime = OffsetDateTime.now
) derives DbCodec
