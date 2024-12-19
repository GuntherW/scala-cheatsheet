package de.wittig.database.magnum

import com.augustnagro.magnum.*

import java.util.UUID

@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
case class MultId(
    @Id id: UUID,
    @Id name: String,
    email: String,
) derives DbCodec
