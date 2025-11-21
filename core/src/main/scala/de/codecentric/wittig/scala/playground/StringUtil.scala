package de.codecentric.wittig.scala.playground

@main
def stringUtil(): Unit =

  println(camelcase("ab_dr_lkj"))
  println(snakecase("abDrLkj"))
  println("[" + padLeft("a", 5) + "]")
  println("[" + padLeft("dfa", 5) + "]")

  def camelcase(s: String): String =
    s.split("_").toList match
      case head :: tail => head + tail.map(_.capitalize).mkString
      case x            => s

  def snakecase(s: String): String =
    s.flatMap {
      case c if c.isUpper => s"_${c.toLower}"
      case c              => c.toString
    }

  def padLeft(s: String, i: Int) = s.padTo(i, " ").mkString
