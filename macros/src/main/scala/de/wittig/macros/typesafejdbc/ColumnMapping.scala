package de.wittig.macros.typesafejdbc

trait ColumnMapping[T <: JdbcType.TL, N <: JdbcNullability.TL, C <: String]:
  type Result // left abstract, will be inferred by the compiler
  def reader: JdbcReader[Result]

object ColumnMapping:
  import JdbcNullability.TL.*

  type Aux[T <: JdbcType.TL, N <: JdbcNullability.TL, C <: String, R] =
    ColumnMapping[T, N, C] { type Result = R }

  given [C <: String]: ColumnMapping[JdbcType.TL.Integer, NonNullable, C] with {
    type Result = Int
    override def reader: JdbcReader[Int] = JdbcReader.int
  }

  given [C <: String]: ColumnMapping[JdbcType.TL.Varchar, NonNullable, C] with {
    type Result = String
    override def reader: JdbcReader[String] = JdbcReader.string
  }

  given [C <: String]: ColumnMapping[JdbcType.TL.Double, NonNullable, C] with {
    type Result = Double
    override def reader: JdbcReader[Double] = JdbcReader.double
  }

  given [C <: String]: ColumnMapping[JdbcType.TL.Boolean, NonNullable, C] with {
    type Result = Boolean
    override def reader: JdbcReader[Boolean] = JdbcReader.boolean
  }

  // nullable column mappings
  given [C <: String, T <: JdbcType.TL, R](using m: Aux[T, NonNullable, C, R]): ColumnMapping[T, Nullable, C] with {
    type Result = Option[R]
    override def reader: JdbcReader[Result] = m.reader.toOption
  }

  // array column mapping
  given [C <: String, T <: JdbcType.TL, R](using m: Aux[T, NonNullable, C, R]): ColumnMapping[JdbcType.TL.Array[T], NonNullable, C] with {
    type Result = List[R]
    override def reader: JdbcReader[Result] = m.reader.toList
  }
