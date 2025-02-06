package de.wittig.macros.typesafejdbc

object JdbcType:
  // Value Level
  enum VL:
    case Integer, Double, Boolean, Varchar
    case Array(typ: JdbcType.VL)
    case NotSupported

  // Type Level
  sealed trait TL
  object TL:
    sealed trait Integer                 extends JdbcType.TL
    sealed trait Double                  extends JdbcType.TL
    sealed trait Boolean                 extends JdbcType.TL
    sealed trait Varchar                 extends JdbcType.TL
    sealed trait Array[T <: JdbcType.TL] extends JdbcType.TL
    sealed trait NotSupported            extends JdbcType.TL
