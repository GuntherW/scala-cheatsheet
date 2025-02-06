package de.wittig.macros.typesafejdbc

object JdbcNullability:

  enum VL:
    case Nullable, NonNullable

  sealed trait TL
  object TL:
    sealed trait Nullable    extends JdbcNullability.TL
    sealed trait NonNullable extends JdbcNullability.TL
