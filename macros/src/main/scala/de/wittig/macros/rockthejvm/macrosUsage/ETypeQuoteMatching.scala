package de.wittig.macros.rockthejvm.macrosUsage
import de.wittig.macros.rockthejvm.macros.ETypeQuoteMatching.*
import scala.util.Try

@main
def eTypeQuoteMatching(): Unit =

  val intDescriptor      = matchType[Int]
  val stringDescriptor   = matchType[String]
  val listDescriptor     = matchType[List[Int]]
  val eitherDescriptor   = matchType[Either[String, Int]]
  val functionDescriptor = matchType[Int => String]
  val tupleDescriptor    = matchType[(Int, String, Int)]
  val tryDescriptor      = matchType[Try[Int]]
  val try2Descriptor     = matchType[Try[String]] // Try[String] is a subtype of Try[AnyVal]
