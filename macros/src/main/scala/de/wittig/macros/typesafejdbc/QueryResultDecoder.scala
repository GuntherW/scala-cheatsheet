package de.wittig.macros.typesafejdbc
import scala.quoted.*

trait QueryResultDecoder[A]:
  def decode(row: Row): A

object QueryResultDecoder:

  // transparent inline def run(inline query: String): List[?] =
  //   val decoder = make(query)
  //   val rows    = JdbcCommunication.runQuery(query)
  //   rows.map(decoder.decode)
  transparent inline def run(inline query: String): List[?]                = ${ runImpl('query) }
  private def runImpl(query: Expr[String])(using q: Quotes): Expr[List[?]] =
    val schema               = JdbcCommunication.getSchema(query.valueOrAbort)
    val descriptorToMappings = schema.values.map(descriptorToMapping)
    val refinedType          = makeRefinedType(descriptorToMappings)
    val columnReaders        = getColumnReaders(descriptorToMappings)
    val decoder              = makeDecoder(columnReaders, refinedType)
    refinedType match
      case '[t] => '{ JdbcCommunication.runQuery($query).map($decoder.decode).asInstanceOf[List[t]] }

  transparent inline def make(inline query: String): QueryResultDecoder[?] =
    ${ makeImpl('query) }

  private def makeImpl(query: Expr[String])(using q: Quotes): Expr[QueryResultDecoder[?]] =

    // 1. Find the Schema
    val schema = JdbcCommunication.getSchema(query.valueOrAbort)

    // 2. Get all column mappings for all column descriptos in the schema
    val descriptorToMappings = schema.values.map(descriptorToMapping)

    // 3. Build the correnct type refinement
    val refinedType = makeRefinedType(descriptorToMappings)

    // 4. Get the column readers
    val columnReaders = getColumnReaders(descriptorToMappings)

    // 5. get the query decoder
    makeDecoder(columnReaders, refinedType)

  // fetches the correct given ColumnMapping for this column
  private def descriptorToMapping(descriptor: ColumnDescriptor)(using q: Quotes): DescriptorToMapping =
    // JdbcType.VL.Varchar => Type[JdbcType.TL.Varchar]
    val jdbcType = toType(descriptor.jdbcType)
    val nullable = toType(descriptor.nullability)
    val colType  = toType(descriptor.name)

    (jdbcType, nullable, colType) match
      case (
            '[type t <: JdbcType.TL; `t`],
            '[type n <: JdbcNullability.TL; `n`],
            '[type c <: String; `c`]
          ) =>
        // fetch a given ColumnMapping
        val mapping = Expr.summon[ColumnMapping[t, n, c]].getOrElse(produceColumnMappingError[t, n, c])
        DescriptorToMapping(descriptor, mapping)

  private def produceColumnMappingError[T: Type, N: Type, C: Type](using q: Quotes) =
    import q.reflect.*
    given Printer[TypeRepr] = Printer.TypeReprShortCode
    val tType               = TypeRepr.of[T].show
    val nType               = TypeRepr.of[N].show
    val cType               = TypeRepr.of[C].show
    report.errorAndAbort(s"Failed to summon a given ColumnMapping[$tType, $nType, $cType]")

  // create a QueryResult based on all column descriptors and mappings
  private def makeRefinedType(descriptorToMappings: List[DescriptorToMapping])(using q: Quotes): Type[?] =
    import q.reflect.*
    val refined = descriptorToMappings.foldLeft(TypeRepr.of[QueryResult]) {
      case (currentRefined, dtm) =>
        // add a new field to currentRefined of the form "val $name: $type"
        val name     = dtm.descriptor.name
        val typeRepr = dtm.mapping match
          case '{ $mapping: ColumnMapping.Aux[_, _, _, colType] } => TypeRepr.of[colType]
        Refinement(currentRefined, name, typeRepr)
    }
    refined.asType

  // fetches all readers of the correct type, so the values can be read correctly into the types
  private def getColumnReaders(descriptorToMappings: List[DescriptorToMapping])(using q: Quotes): Expr[List[(String, JdbcReader[?])]] =
    val jdbcReaders = descriptorToMappings.map { dtm =>
      val nameExpr = Expr(dtm.descriptor.name)
      dtm.mapping match
        case '{ $mapping: ColumnMapping.Aux[_, _, _, colType] } => '{ $nameExpr -> $mapping.reader }

    }
    Expr.ofList(jdbcReaders)

  // get the final decoder that can read entire rows into the structural type inferred ealier
  private def makeDecoder(columnReaders: Expr[List[(String, JdbcReader[?])]], refinedType: Type[?])(using q: Quotes): Expr[QueryResultDecoder[?]] =
    refinedType match
      case '[r] => '{
          new QueryResultDecoder[r] {
            override def decode(row: Row): r = QueryResult($columnReaders)(row).asInstanceOf[r]
          }
        }

  private def toType(vlType: JdbcType.VL)(using q: Quotes): Type[? <: JdbcType.TL] =
    vlType match
      case JdbcType.VL.Boolean        => Type.of[JdbcType.TL.Boolean]
      case JdbcType.VL.Double         => Type.of[JdbcType.TL.Double]
      case JdbcType.VL.Integer        => Type.of[JdbcType.TL.Integer]
      case JdbcType.VL.Varchar        => Type.of[JdbcType.TL.Varchar]
      case JdbcType.VL.Array(content) =>
        toType(content) match
          case '[type i <: JdbcType.TL; `i`] => Type.of[JdbcType.TL.Array[i]]
      case JdbcType.VL.NotSupported   => Type.of[JdbcType.TL.NotSupported]

  private def toType(vlType: JdbcNullability.VL)(using q: Quotes): Type[? <: JdbcNullability.TL] =
    vlType match
      case JdbcNullability.VL.NonNullable => Type.of[JdbcNullability.TL.NonNullable]
      case JdbcNullability.VL.Nullable    => Type.of[JdbcNullability.TL.Nullable]

  private def toType(name: String)(using q: Quotes): Type[?] =
    import q.reflect.*
    ConstantType(StringConstant(name)).asType

  case class DescriptorToMapping(
      descriptor: ColumnDescriptor,
      mapping: Expr[ColumnMapping[?, ?, ?]]
  )
