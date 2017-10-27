package de.codecentric.wittig.scala.cats.validated

import enumeratum._
import enumeratum.values._

sealed abstract class Error(
    val value: Int,
    msg: Option[String] = None, // For Developers
    values: Seq[AnyRef] = Seq.empty
) extends IntEnumEntry

object Error extends IntEnum[Error] {

  val values = findValues

  case object InvalidEmail extends Error(1)
  case object InvalidPhone extends Error(2)
  case object InvalidAge extends Error(3)
  case object InvalidRank extends Error(4)
  case object InvalidAgeBiggerRank extends Error(5)
}
