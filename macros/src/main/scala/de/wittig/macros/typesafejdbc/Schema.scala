package de.wittig.macros.typesafejdbc

import java.sql.ResultSetMetaData
import java.sql.Types

case class ColumnDescriptor(
    index: Int,
    name: String,
    jdbcType: JdbcType.VL,
    nullability: JdbcNullability.VL
)

final case class Schema(values: List[ColumnDescriptor])
object Schema:
  def fromMetadata(metadata: ResultSetMetaData): Schema =
    val descriptors =
      for
        i <- 1 to metadata.getColumnCount
      yield ColumnDescriptor(
        index = i,
        name = metadata.getColumnLabel(i),
        jdbcType = getType(metadata, i),
        nullability = getNullable(metadata, i),
      )
    Schema(descriptors.toList)

  private def getType(metadata: ResultSetMetaData, i: Int): JdbcType.VL =
    metadata.getColumnType(i) match
      case Types.VARCHAR                                                    => JdbcType.VL.Varchar
      case Types.INTEGER                                                    => JdbcType.VL.Integer
      case Types.DOUBLE                                                     => JdbcType.VL.Double
      case Types.BOOLEAN                                                    => JdbcType.VL.Boolean
      case Types.ARRAY if metadata.getColumnTypeName(i).contains("varchar") => JdbcType.VL.Array(JdbcType.VL.Varchar)
      case _                                                                => JdbcType.VL.NotSupported

  private def getNullable(metadata: ResultSetMetaData, i: Int): JdbcNullability.VL =
    metadata.isNullable(i) match
      case ResultSetMetaData.columnNoNulls => JdbcNullability.VL.NonNullable
      case _                               => JdbcNullability.VL.Nullable
