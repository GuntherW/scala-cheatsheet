package de.codecentric.wittig.scala.migration

import de.codecentric.wittig.scala.migration.Issue.{BILLABLE, Types}
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.jawn.decode
import io.circe.syntax.*

case class A(types: Types, name: String)

object A {
  implicit val enc: Encoder[A] = deriveEncoder
  implicit val dec: Decoder[A] = deriveDecoder
}

object Main extends App:

  println("Hallo Welt")

  def divOrElse(a: BigDecimal, b: BigDecimal): BigDecimal = a / b
  val Zero: BigDecimal                                    = BigDecimal(0)

  val l = 3L
  println(l.divOrElse(4L, Zero))

  implicit final class LongSafeDivOps(private val long: Long) extends AnyVal {

    def divOrElse(divisor: BigDecimal, whenZero: => BigDecimal): BigDecimal =
      divisor match {
        case Zero => whenZero
        case _    => BigDecimal(long) / divisor
      }
  }

  val a = A(BILLABLE, "lkjl")
  println(a.asJson)

  val s  = """{
            |  "types" : "Vacatio",
            |  "name" : "lkjl"
            |}
            |""".stripMargin
  val aa = decode[A](s)
  println(aa)

object Issue {

  sealed abstract class Types(val value: String)

  object Types {
    val values               = List(
      BILLABLE,
      ADMIN,
      VACATION,
      PRESALES,
      RECRUITING,
      ILLNESS,
      TRAVELTIME,
      OFFPROJECT,
      ABSENCE,
      PARENTALLEAVE,
      INNOVATIONBUDGET,
      ACTIVE_ON_CALL,
      PASSIVE_ON_CALL,
      OTHER,
    )
    def of(s: String): Types = values.find(_.value == s).get
  }

  object BILLABLE         extends Types("Billable")
  object ADMIN            extends Types("Admin Time")
  object VACATION         extends Types("Vacation")
  object PRESALES         extends Types("Pre-Sales")
  object RECRUITING       extends Types("Recruiting")
  object ILLNESS          extends Types("Illness")
  object TRAVELTIME       extends Types("Travel Time")
  object OFFPROJECT       extends Types("OffProject Time")
  object ABSENCE          extends Types("Other absence")
  object PARENTALLEAVE    extends Types("Parental leave")
  object INNOVATIONBUDGET extends Types("Innovation budget")
  object ACTIVE_ON_CALL   extends Types("Active On-Call")
  object PASSIVE_ON_CALL  extends Types("Passive On-Call")
  object OTHER            extends Types("Other")

  implicit def issueTypeEnc: Encoder[Types] = Encoder.encodeString.contramap(_.value)
  implicit def issueTypeDec: Decoder[Types] = Decoder.decodeString.map(Types.of)
}
