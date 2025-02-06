package de.wittig.macros.typesafejdbc

object JdbcNullability:

  enum VL:
    case Nullable, NonNullable

  sealed trait TL
  object TL:
    trait Nullable    extends JdbcNullability.TL
    trait NonNullable extends JdbcNullability.TL
