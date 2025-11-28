package de.wittig.client.generated

import caliban.client.CalibanClientError.DecodingError
import caliban.client.FieldBuilder.*
import caliban.client.*
import caliban.client.__Value.*

object Client {

  sealed trait Origin extends scala.Product with scala.Serializable { def value: String }
  object Origin {
    case object BELT  extends Origin { val value: String = "BELT"  }
    case object EARTH extends Origin { val value: String = "EARTH" }
    case object MARS  extends Origin { val value: String = "MARS"  }

    implicit val decoder: ScalarDecoder[Origin] = {
      case __StringValue("BELT")  => Right(Origin.BELT)
      case __StringValue("EARTH") => Right(Origin.EARTH)
      case __StringValue("MARS")  => Right(Origin.MARS)
      case other                  => Left(DecodingError(s"Can't build Origin from input $other"))
    }
    implicit val encoder: ArgEncoder[Origin]    = {
      case Origin.BELT  => __EnumValue("BELT")
      case Origin.EARTH => __EnumValue("EARTH")
      case Origin.MARS  => __EnumValue("MARS")
    }

    val values: scala.collection.immutable.Vector[Origin] = scala.collection.immutable.Vector(BELT, EARTH, MARS)
  }

  type Character
  object Character {
    def name: SelectionBuilder[Character, String]            = _root_.caliban.client.SelectionBuilder.Field("name", Scalar())
    def nicknames: SelectionBuilder[Character, List[String]] = _root_.caliban.client.SelectionBuilder.Field("nicknames", ListOf(Scalar()))
    def origin: SelectionBuilder[Character, Origin]          = _root_.caliban.client.SelectionBuilder.Field("origin", Scalar())
  }

  type Query = _root_.caliban.client.Operations.RootQuery
  object Query {
    def characters[A](origin: scala.Option[Origin] = None)(innerSelection: SelectionBuilder[Character, A])(implicit
        encoder0: ArgEncoder[scala.Option[Origin]]
    ): SelectionBuilder[_root_.caliban.client.Operations.RootQuery, List[A]]         =
      _root_.caliban.client.SelectionBuilder.Field("characters", ListOf(Obj(innerSelection)), arguments = List(Argument("origin", origin, "Origin")(using encoder0)))
    def character[A](name: String)(innerSelection: SelectionBuilder[Character, A])(implicit
        encoder0: ArgEncoder[String]
    ): SelectionBuilder[_root_.caliban.client.Operations.RootQuery, scala.Option[A]] =
      _root_.caliban.client.SelectionBuilder.Field("character", OptionOf(Obj(innerSelection)), arguments = List(Argument("name", name, "String!")(using encoder0)))
  }

}
