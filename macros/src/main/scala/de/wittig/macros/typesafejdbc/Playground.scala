package de.wittig.macros.typesafejdbc
import scala.reflect.Selectable.reflectiveSelectable

object PlaygroundSchema extends App:

  val schema = JdbcCommunication.getSchema("select * from users")
  schema.values.foreach(println)

object PlaygroundColumnMapping extends App:

  def getValues[T <: JdbcType.TL, N <: JdbcNullability.TL, C <: String](rows: List[Row], colName: String)(using mapper: ColumnMapping[T, N, C]): List[mapper.Result] =
    rows
      .map(_.values)
      .map(_.apply(colName))
      .map(v => mapper.reader.read(v))

  val rows    = JdbcCommunication.runQuery("select * from users")
  rows.foreach(println)
  val names   = getValues[JdbcType.TL.Varchar, JdbcNullability.TL.NonNullable, "name"](rows, "name")
  names.foreach(println)
  val ages    = getValues[JdbcType.TL.Integer, JdbcNullability.TL.Nullable, "age"](rows, "age")
  ages.foreach(println)
  val hobbies = getValues[JdbcType.TL.Array[JdbcType.TL.Varchar], JdbcNullability.TL.NonNullable, "hobbies"](rows, "hobbies")
  hobbies.foreach(println)

object PlaygroundQueryDecoder extends App:
  inline val query = "select * from users"
  val decoder      = QueryResultDecoder.make(query)
  val rows         = JdbcCommunication.runQuery(query)
  val typedRows    = rows.map(decoder.decode)
  val names        = typedRows.map(_.name)
  names.foreach(println)

object PlaygroundQuery2Decoder extends App:
  inline val query = "select * from users"
  val typedRows    = QueryResultDecoder.run(query)
  typedRows.map(_.name).foreach(println)
