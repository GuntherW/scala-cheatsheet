package de.codecentric.wittig.scala.regex

import scala.util.matching.Regex

/** @author
  *   gunther
  */
object RegexTester extends App:

  private val Date  = """(\d{4})-(\d{2})-(\d{2})""".r
  private val dates = "Date: 2004-01-20 17:25:18 GMT (10 years, 28 weeks, 5 days, 17 hours and 51 minutes ago)"

  private val s1 = "2004-01-20" match
    case Date(year, _, _) => s"$year was a good year"
    case _                => "no match found"
  assert(s1 == "2004 was a good year")

  // In a pattern match, Regex normally matches the entire input. However, an unanchored Regex finds the pattern anywhere in the input.
  private val EmbeddedDate = Date.unanchored
  private val s2           = dates match
    case EmbeddedDate(year, "01", "20") => year
    case _                              => "nicht gematched"
  assert(s2 == "2004")

  private val s3 = Date.findFirstIn(dates).getOrElse("nicht gematched")
  assert(s3 == "2004-01-20")

  // Group names
  private val DatePatternWithGroup  = new Regex("""(\d{4})-(\d{2})-(\d{2})""", "year", "month", "day")
  private val DatePatternWithGroup2 = """(?<year>\d{4})-(?<month>\d{2})-(?<day>\d{2})""".r
  private val s4                    = for m <- DatePatternWithGroup.findFirstMatchIn(dates) yield m.group("year")
  assert(s4.contains("2004"))
  private val s4b                   = for m <- DatePatternWithGroup2.findFirstMatchIn(dates) yield m.group("year")
  assert(s4b.contains("2004"))

  // Find all:
  private val s5 = for m <- Date findAllMatchIn dates yield m group 1
  assert(s5.toList == List("2004"))

  private val dates2 = "Date: 2004-01-20 1950-01-20 1960-01-20 "
  private val mi     = Date.findAllIn(dates2)
  private val s6     = mi.filter(_ => mi.group(1).toInt < 1960)
  assert(s6.toList == List("1950-01-20"))

  // Replacement:
  private val s7 = Date.replaceAllIn(dates, _.group(1))
  assert(s7 == "Date: 2004 17:25:18 GMT (10 years, 28 weeks, 5 days, 17 hours and 51 minutes ago)")

  // Apply a Match to a different Pattern
  private val docSpree = """2004(?:-\d{2}){2}""".r
  private val docView  = Date.replaceAllIn(
    dates,
    _ match
      case docSpree() => "Historic doc spree!"
      case _          => "Something else happened"
  )
  assert(docView == "Date: Historic doc spree! 17:25:18 GMT (10 years, 28 weeks, 5 days, 17 hours and 51 minutes ago)")

  private val VersionPattern                       = """(\d+)(?:\.(\d+)(?:\.(\d+))?)?""".r
  private def full(value: String): Option[Version] = value match
    case VersionPattern(major, null, null)   => Some(Version(major.toInt))
    case VersionPattern(major, minor, null)  => Some(Version(major.toInt, minor.toInt))
    case VersionPattern(major, minor, patch) => Some(Version(major.toInt, minor.toInt, patch.toInt))
    case _                                   => None
  assert(full("1.2.3").contains(Version(1, 2, 3)))
  assert(full("1.2").contains(Version(1, 2)))
  assert(full("1").contains(Version(1)))
  assert(full("1.").isEmpty)

case class Version(major: Int, minor: Int, patch: Int) {
  override def toString: String = this match
    case Version(_, -1, _) => major.toString
    case Version(_, _, -1) => s"$major.$minor"
    case _                 => s"$major.$minor.$patch"
}

object Version {
  def apply(major: Int, minor: Int): Version = Version(major, minor, -1)
  def apply(major: Int): Version             = Version(major, -1, -1)
}
