package de.codecentric.wittig.scala.regex

import scala.util.matching.Regex

@main
def main(): Unit =

  // Define a regex with a positive lookahead
  val pattern: Regex          = "(?=b).*d".r
  val patternNonGreedy: Regex = "(?=b).*?d".r

  val matches = pattern.findAllIn("abcde")
  assert(matches.toList == List("bcd"))

  val matchesGreedy = pattern.findAllIn("abcdeabxde")
  assert(matchesGreedy.toList == List("bcdeabxd"))

  val matchesNonGreedy = patternNonGreedy.findAllIn("abcdeabxde")
  assert(matchesNonGreedy.toList == List("bcd", "bxd"))
