package agent

import io.circe.{Decoder, Json}
import io.circe.derivation.{Configuration, ConfiguredCodec}
import io.circe.syntax.*
import sttp.ai.claude.models.{PropertySchema, Tool, ToolInputSchema}
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import scala.util.{Failure, Success, Try}

/** JSON-Feldnamen der Ein-/Ausgabetypen sollen `snake_case` folgen (passend zu den Tool-Schemas), die Scala-Felder selbst `camelCase` (Projekt-Konvention).
  */
given Configuration = Configuration.default.withSnakeCaseMemberNames

/** Gemeinsamer Decode-/Fehlerbehandlungs-Baustein für alle Tool-Handler unten: dekodiert `rawInput` nach `A` und liefert bei Erfolg `onInput(input)`, bei einem Decode-Fehler ein einheitliches
  * `{"error": "..."}`-JSON. Vermeidet die identische `Left`/`Right`-Fallunterscheidung in jedem einzelnen Handler.
  */
private def decodeInput[A: Decoder](toolName: String, rawInput: Map[String, Json])(onInput: A => String): String =
  Json.fromFields(rawInput).as[A] match
    case Left(error)  => Json.obj("error" -> s"Konnte $toolName-Eingabe nicht parsen: ${error.getMessage}".asJson).noSpaces
    case Right(input) => onInput(input)

/** Tool 1: `get_current_time` - liefert Datum/Uhrzeit für eine optionale Zeitzone (IANA-ID, z. B. "Europe/Berlin"). Ohne Angabe wird UTC verwendet.
  */
case class CurrentTimeInput(timezone: Option[String]) derives ConfiguredCodec
case class CurrentTimeResult(timezone: String, iso8601: String) derives ConfiguredCodec

object CurrentTimeTool:

  val definition: Tool.Custom = Tool(
    name = "get_current_time",
    description = "Liefert das aktuelle Datum und die aktuelle Uhrzeit für eine Zeitzone (IANA-Zeitzonen-ID, z. B. 'Europe/Berlin'). Ohne Angabe wird UTC verwendet.",
    inputSchema = ToolInputSchema.forObject(
      properties = Map(
        "timezone" -> PropertySchema.string("IANA-Zeitzonen-ID, z. B. 'Europe/Berlin' oder 'America/New_York'. Optional, Standard: UTC."),
      ),
      required = None,
    ),
  )

  def handler(rawInput: Map[String, Json]): String =
    decodeInput[CurrentTimeInput](definition.name, rawInput) { input =>
      val zoneId = input.timezone.getOrElse("UTC")
      Try(java.time.ZoneId.of(zoneId)) match
        case Failure(_)    => Json.obj("error" -> s"Unbekannte Zeitzone: '$zoneId'".asJson).noSpaces
        case Success(zone) =>
          val now = ZonedDateTime.now(zone)
          CurrentTimeResult(timezone = zoneId, iso8601 = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)).asJson.noSpaces
    }

/** Tool 2: `calculator` - wertet einen einfachen arithmetischen Ausdruck aus (+, -, *, /, Klammern). Bewusst simpel gehalten (keine Variablen/Funktionen) - dient nur der Veranschaulichung.
  */
case class CalculatorInput(expression: String) derives ConfiguredCodec
case class CalculatorResult(expression: String, result: Double) derives ConfiguredCodec

object CalculatorTool:

  val definition: Tool.Custom = Tool(
    name = "calculator",
    description = "Wertet einen einfachen arithmetischen Ausdruck aus (Zahlen, +, -, *, /, Klammern), z. B. '(3 + 4) * 2'.",
    inputSchema = ToolInputSchema.forObject(
      properties = Map(
        "expression" -> PropertySchema.string("Arithmetischer Ausdruck, z. B. '(3 + 4) * 2'."),
      ),
      required = Some(List("expression")),
    ),
  )

  def handler(rawInput: Map[String, Json]): String =
    decodeInput[CalculatorInput](definition.name, rawInput) { input =>
      Try(ExpressionParser.evaluate(input.expression)) match
        case Failure(e)      => Json.obj("error" -> s"Konnte Ausdruck nicht auswerten: ${e.getMessage}".asJson).noSpaces
        case Success(result) => CalculatorResult(expression = input.expression, result = result).asJson.noSpaces
    }

/** Winziger, rekursiver Parser/Evaluator für arithmetische Ausdrücke (+, -, *, /, Klammern, unäres Minus). Bewusst ohne externe Bibliothek - dieses Lernprojekt soll den Tool-Search-Flow zeigen, nicht
  * einen produktionsreifen Parser. Die Zeigerposition `pos` bleibt bewusst ein lokal gekapselter `var` (kein sichtbarer Zustand außerhalb dieser Methode) - ein rein immutabler State-Thread würde hier
  * gegenüber dem winzigen Scope unnötige Komplexität hinzufügen.
  */
object ExpressionParser:
  def evaluate(expr: String): Double =
    val chars = expr.filterNot(_.isWhitespace)
    var pos   = 0

    def peek: Option[Char]              = if pos < chars.length then Some(chars(pos)) else None
    def advance(): Char                 = { val c = chars(pos); pos += 1; c }
    def expectAndAdvance(c: Char): Unit =
      if peek.contains(c) then advance() else throw new IllegalArgumentException(s"Erwartete '$c' an Position $pos in '$expr'")

    def parseNumber(): Double =
      val start = pos
      while peek.exists(c => c.isDigit || c == '.') do advance()
      if pos == start then throw new IllegalArgumentException(s"Zahl erwartet an Position $pos in '$expr'")
      chars.substring(start, pos).toDouble

    def parseFactor(): Double =
      peek match
        case Some('(') =>
          advance()
          val value = parseExpr()
          expectAndAdvance(')')
          value
        case Some('-') => advance(); -parseFactor()
        case Some('+') => advance(); parseFactor()
        case _         => parseNumber()

    def parseBinaryLevel(next: () => Double, ops: Map[Char, (Double, Double) => Double]): Double =
      var value = next()
      while peek.exists(ops.contains) do
        val op = advance()
        value = ops(op)(value, next())
      value

    def parseTerm(): Double = parseBinaryLevel(parseFactor, Map('*' -> (_ * _), '/' -> (_ / _)))
    def parseExpr(): Double = parseBinaryLevel(parseTerm, Map('+' -> (_ + _), '-' -> (_ - _)))

    val result = parseExpr()
    if pos != chars.length then throw new IllegalArgumentException(s"Unerwartetes Zeichen an Position $pos in '$expr'")
    result

/** Tool 3: `roll_dice` - würfelt `count`-mal einen Würfel mit `sides` Seiten und liefert die Einzelwürfe sowie die Summe.
  */
case class RollDiceInput(sides: Int, count: Int) derives ConfiguredCodec
case class RollDiceResult(sides: Int, count: Int, rolls: List[Int], sum: Int) derives ConfiguredCodec

object RollDiceTool:

  val definition: Tool.Custom = Tool(
    name = "roll_dice",
    description = "Würfelt 'count'-mal einen Würfel mit 'sides' Seiten und liefert die Einzelwürfe sowie die Summe.",
    inputSchema = ToolInputSchema.forObject(
      properties = Map(
        "sides" -> PropertySchema.integer("Anzahl der Seiten des Würfels, z. B. 6 oder 20."),
        "count" -> PropertySchema.integer("Anzahl der Würfe."),
      ),
      required = Some(List("sides", "count")),
    ),
  )

  def handler(rawInput: Map[String, Json]): String =
    decodeInput[RollDiceInput](definition.name, rawInput) {
      case input if input.sides < 2 || input.count < 1 =>
        Json.obj("error" -> "sides muss >= 2 und count muss >= 1 sein.".asJson).noSpaces
      case input                                       =>
        val rolls = List.fill(input.count)(scala.util.Random.nextInt(input.sides) + 1)
        RollDiceResult(sides = input.sides, count = input.count, rolls = rolls, sum = rolls.sum).asJson.noSpaces
    }
