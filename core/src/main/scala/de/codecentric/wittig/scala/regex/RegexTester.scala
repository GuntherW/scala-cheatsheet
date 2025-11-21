package de.codecentric.wittig.scala.regex

import scala.util.matching.Regex

/** @author
  *   gunther
  */
@main
def regexTester(): Unit =

  val datePattern           = """(\d{4})-(\d{2})-(\d{2})""".r
  val datePatternUnanchored = datePattern.unanchored // In a pattern match, Regex normally matches the entire input. However, an unanchored Regex finds the pattern anywhere in the input.
  val date                  = "2004-01-20"
  val dates                 = "Date: 2004-01-20 17:25:18 GMT (10 years, 28 weeks, 5 days, 17 hours and 51 minutes ago)"

  val s1 = date match
    case datePattern(year, _, _) => s"$year was a good year"
    case _                       => "no match found"
  assert(s1 == "2004 was a good year")

  val s2 = dates match
    case datePatternUnanchored(year, "01", "20") => year
    case _                                       => "nicht gematched"
  assert(s2 == "2004")

  val s3 = datePattern.findFirstIn(dates).getOrElse("nicht gematched")
  assert(s3 == date)

  // Group names
  val yearGroupName         = "year"
  val DatePatternWithGroup1 = Regex("""(\d{4})-(\d{2})-(\d{2})""", "year", "month", "day")
  val DatePatternWithGroup2 = s"""(?<$yearGroupName>\\d{4})-(?<month>\\d{2})-(?<day>\\d{2})""".r
  val DatePatternWithGroup3 = """(?<year>\d{4})-(?<month>\d{2})-(?<day>\d{2})""".r

  val s4  = DatePatternWithGroup1.findFirstMatchIn(dates).map(_.group(yearGroupName))
  assert(s4.contains("2004"))
  val s4b = DatePatternWithGroup2.findFirstMatchIn(dates).map(_.group(yearGroupName))
  assert(s4b.contains("2004"))
  val s4c = DatePatternWithGroup3.findFirstMatchIn(dates).map(_.group(yearGroupName))
  assert(s4c.contains("2004"))

  // Find all:
  val s5 = datePattern.findAllMatchIn(dates).map(_.group(1))
  assert(s5.toList == List("2004"))

  val dates2 = "Date: 2004-01-20 1950-01-20 1960-01-20 "
  val mi     = datePattern.findAllIn(dates2)
  val s6     = mi.filter(_ => mi.group(1).toInt < 1960)
  assert(s6.toList == List("1950-01-20"))

  // Replacement:
  val s7 = datePattern.replaceAllIn(dates, _.group(1))
  assert(s7 == "Date: 2004 17:25:18 GMT (10 years, 28 weeks, 5 days, 17 hours and 51 minutes ago)")

  // Apply a Match to a different Pattern
  val docSpree = """2004(?:-\d{2}){2}""".r
  val docView  = datePattern.replaceAllIn(
    dates,
    _ match
      case docSpree() => "Historic doc spree!"
      case _          => "Something else happened"
  )
  assert(docView == "Date: Historic doc spree! 17:25:18 GMT (10 years, 28 weeks, 5 days, 17 hours and 51 minutes ago)")
