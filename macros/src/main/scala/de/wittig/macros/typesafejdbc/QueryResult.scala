package de.wittig.macros.typesafejdbc

class QueryResult(colReaders: List[(String, JdbcReader[?])])(row: Row) extends Selectable:
  private val namedReaders: Map[String, JdbcReader[?]] = colReaders.toMap

  def selectDynamic(name: String): Any =
    val reader = namedReaders.getOrElse(name, throw new RuntimeException(s"invalid column selected: $name"))
    val value  = row.values.getOrElse(name, throw new RuntimeException(s"invalid column selected: $name"))
    reader.read(value)
