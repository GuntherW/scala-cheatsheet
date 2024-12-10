package de.codecentric.wittig.scala.playground

object StringUtil extends App:

  println(camelcase("ab_dr_lkj"))
  println("[" + padLeft("a", 5) + "]")
  println("[" + padLeft("dfa", 5) + "]")

  def camelcase(s: String): String =
    (s.split("_").toList match
      case head :: tail => head :: tail.map(_.capitalize)
      case x            => x
    ).mkString

  def snakecase(s: String): String =
    s.foldLeft(new StringBuilder) {
      case (s, c) if Character.isUpperCase(c) => s.append("_").append(Character.toLowerCase(c))
      case (s, c)                             => s.append(c)
    }.toString

  def padLeft(s: String, i: Int) = s.padTo(i, " ").mkString
