package de.wittig.parsercombinators

import java.text.NumberFormat
import java.util.Locale
import scala.util.parsing.combinator.JavaTokenParsers

object RezeptParser extends JavaTokenParsers {

  override val whiteSpace = """(\s|#.*#)+""".r

  def liter         = wholeNumber <~ "l|Liter".r ^^ (_.toInt)
  def gramm         = wholeNumber <~ "g|Gramm".r ^^ (_.toInt)
  def grad          = wholeNumber <~ "°|Grad".r ~ "C|Celsius".r ^^ (_.toInt)
  def minuten       = wholeNumber <~ "min|Min|Minuten".r ^^ (_.toInt)
  def index         = wholeNumber <~ "." ^^ (_.toInt)
  def string        = """[a-zA-ZäÄüÜöÖß\s]*""".r
  def dezimal       = """(\d+(,\d*)?|\d*,\d+)""".r ^^ (d => NumberFormat.getInstance(Locale.GERMANY).parse(d).doubleValue)
  def alphasaeure   = dezimal <~ "% Alpha"
  def name          = "Name:" ~> string
  def menge         = "Menge:" ~> liter
  def schuett       = string ~ gramm ^^ { case malzname ~ menge => Schuettung(malzname.trim, menge) }
  def schuettung    = "Schüttung:" ~> rep(schuett)
  def brauwasser    = "Brauwasser:" ~> liter ~ "Hauptguss" ~ liter <~ "Nachguss" ^^ { case hg ~ _ ~ ng => Brauwasser(hg, ng) }
  def hopfen        = "Hopfen:" ~> rep(hops)
  def hops          = string ~ alphasaeure ~ gramm ^^ { case name ~ alpha ~ menge => Hopfen(name.trim, alpha, menge) }
  def hefe          = "Hefe:" ~> """.*""".r
  def maischvorgang = "Maischen:" ~> einmaischen ~ rep(rast) ~ ablaeutern ^^ { case ein ~ rast ~ abl => Maischvorgang(ein, rast, abl) }
  def einmaischen   = "Einmaischen" ~ opt("bei") ~> grad
  def rast          = index ~ "Rast bei" ~ grad ~ "für" ~ minuten ^^ { case nummer ~ _ ~ grad ~ _ ~ dauer => Rast(nummer, dauer, grad) }
  def ablaeutern    = "Abläutern" ~ opt("bei") ~> grad

  def rezept = name ~ opt(menge) ~ schuettung ~ brauwasser ~ hopfen ~ hefe ~ maischvorgang ^^ { case name ~ menge ~ schuettung ~ brauwasser ~ hopfen ~ hefe ~ maischen =>
    Rezept(name, menge, schuettung, brauwasser, hopfen, hefe, maischen)
  }

  def apply(toParse: String): ParseResult[Rezept] = parse(rezept, toParse)
}

case class Rezept(name: String, menge: Option[Int], schuettung: List[Schuettung], brauwasser: Brauwasser, hopfen: List[Hopfen], hefe: String, maischvorgang: Maischvorgang)
case class Schuettung(malzname: String, menge: Int)
case class Brauwasser(hauptguss: Int, nachguss: Int)
case class Hopfen(name: String, alphaSaeuregehalt: Double, menge: Int)
case class Maischvorgang(einmaischen: Int, rasten: List[Rast], ablaeutern: Int)
case class Rast(nummer: Int, dauer: Int, grad: Int)
