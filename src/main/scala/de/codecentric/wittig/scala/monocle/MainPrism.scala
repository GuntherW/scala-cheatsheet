package de.codecentric.wittig.scala.monocle

/**
  *A Prism is an optic used to select part of a Sum type (also known as Coproduct), e.g. sealed trait or Enum.
  */
object MainPrism extends App {
  sealed trait Json
  case object JNull                     extends Json
  case class JStr(v: String)            extends Json
  case class JNum(v: Double)            extends Json
  case class JObj(v: Map[String, Json]) extends Json

  import monocle.Prism

  val jStr = Prism[Json, String] {
    case JStr(v) => Some(v)
    case _       => None
  }(JStr)

  assert(jStr("hello") == JStr("hello"))
  assert(jStr.getOption(JStr("hello")).contains("hello"))
  assert(jStr.getOption(JNum(2)).isEmpty)

  import monocle.macros.GenPrism

  val rawJNum: Prism[Json, JNum] = GenPrism[Json, JNum]
  assert(rawJNum.getOption(JNum(4.5)) == Some(JNum(4.5)))
}
