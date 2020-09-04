package de.codecentric.wittig.scala.playground

object StringUtil {
  def camelcase(s: String): String =
    (s.split("_").toList match {
      case head :: tail => head :: tail.map(_.capitalize)
      case x            => x
    }).mkString

  def snakecase(s: String): String =
    s.foldLeft(new StringBuilder) {
      case (s, c) if Character.isUpperCase(c) =>
        s.append("_").append(Character.toLowerCase(c))
      case (s, c)                             =>
        s.append(c)
    }.toString
}
