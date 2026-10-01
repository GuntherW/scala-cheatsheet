package agent

import io.circe.{Decoder, Json}
import io.circe.derivation.{Configuration, ConfiguredCodec}
import io.circe.syntax.*
import sttp.ai.claude.models.Tool
import sttp.apispec.circe.*
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description
import sttp.tapir.docs.apispec.schema.TapirSchemaToJsonSchema

import java.time.ZoneId.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import scala.util.{Failure, Random, Success, Try}

/** JSON-Feldnamen der Ein-/Ausgabetypen sollen `snake_case` folgen (passend zu den Tool-Schemas), die Scala-Felder selbst `camelCase` (Projekt-Konvention).
  */
given Configuration = Configuration.default.withSnakeCaseMemberNames

/** Leitet aus einem per Tapir `derives Schema` annotierten Typ `T` das rohe JSON-Schema ab, das Claude als `inputSchema` eines `Tool.CustomRaw` erwartet. Das ist der von sttp-ai dokumentierte Weg
  * ("the easiest way: derive from a case class", siehe <https://sttp-ai.softwaremill.com/other/json-schemas.html>) - Typ und Schema haben dadurch EINE Quelle der Wahrheit (die Case Class samt
  * `@description`-Annotationen) statt zweier parallel gepflegter Strukturen (Case Class UND separat aufgeschriebene `ToolInputSchema`/`PropertySchema`).
  */
private[agent] def jsonSchemaOf[T](using schema: Schema[T]): Json =
  TapirSchemaToJsonSchema(schema, markOptionsAsNullable = true).asJson

/** Basisklasse für ein Tool, dessen Eingabe als Case Class `A` modelliert ist (abgeleiteter circe `Decoder` + Tapir `Schema`). Nimmt jedem konkreten Tool das manuelle
  * `Json.fromFields(rawInput).as[A]`-Dekodieren samt Fehlerbehandlung ab - Unterklassen implementieren nur noch die fachliche Logik in `run(input: A)`, OHNE jedes Mal einen Decode-Aufruf wie
  * `decodeInput[X](definition.name, rawInput) { input => ... }` zu wiederholen. Ein Decode-Fehler wird automatisch (via circes `Either.fold`) in ein `ToolCallResult.error(...)` übersetzt.
  */
abstract class JsonTool[A](name: String, description: String)(using decoder: Decoder[A], schema: Schema[A]):
  val definition: Tool.CustomRaw = Tool.customRaw(name, description, jsonSchemaOf[A])

  /** Fachliche Tool-Logik - wird nur mit bereits erfolgreich dekodierter Eingabe aufgerufen. */
  protected def run(input: A): ToolCallResult

  final def handler(rawInput: Map[String, Json]): ToolCallResult =
    Json.fromFields(rawInput).as[A](using decoder).fold(error => ToolCallResult.error(s"Konnte $name-Eingabe nicht parsen: ${error.getMessage}"), run)

/** Tool 1: `get_current_time` - liefert Datum/Uhrzeit für eine optionale Zeitzone (IANA-ID, z. B. "Europe/Berlin"). Ohne Angabe wird UTC verwendet.
  */
case class CurrentTimeInput(
    @description("IANA-Zeitzonen-ID, z. B. 'Europe/Berlin' oder 'America/New_York'. Optional, Standard: UTC.")
    timezone: Option[String],
) derives ConfiguredCodec, Schema
case class CurrentTimeResult(timezone: String, iso8601: String) derives ConfiguredCodec

object CurrentTimeTool
    extends JsonTool[CurrentTimeInput](
      name = "get_current_time",
      description = "Liefert das aktuelle Datum und die aktuelle Uhrzeit für eine Zeitzone (IANA-Zeitzonen-ID, z. B. 'Europe/Berlin'). Ohne Angabe wird UTC verwendet.",
    ):

  protected def run(input: CurrentTimeInput): ToolCallResult =
    val zoneId = input.timezone.getOrElse("UTC")
    Try(of(zoneId)) match
      case Failure(_)    => ToolCallResult.error(s"Unbekannte Zeitzone: '$zoneId'")
      case Success(zone) =>
        val now = ZonedDateTime.now(zone)
        ToolCallResult(CurrentTimeResult(timezone = zoneId, iso8601 = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)).asJson.noSpaces)

/** Tool 2: `calculator` - wertet einen einfachen arithmetischen Ausdruck aus (+, -, *, /, Klammern). Bewusst simpel gehalten (keine Variablen/Funktionen) - dient nur der Veranschaulichung.
  */
case class CalculatorInput(
    @description("Arithmetischer Ausdruck, z. B. '(3 + 4) * 2'.")
    expression: String,
) derives ConfiguredCodec, Schema
case class CalculatorResult(expression: String, result: Double) derives ConfiguredCodec

object CalculatorTool
    extends JsonTool[CalculatorInput](
      name = "calculator",
      description = "Wertet einen einfachen arithmetischen Ausdruck aus (Zahlen, +, -, *, /, Klammern), z. B. '(3 + 4) * 2'.",
    ):

  protected def run(input: CalculatorInput): ToolCallResult =
    Try(ExpressionParser.evaluate(input.expression)) match
      case Failure(e)      => ToolCallResult.error(s"Konnte Ausdruck nicht auswerten: ${e.getMessage}")
      case Success(result) => ToolCallResult(CalculatorResult(expression = input.expression, result = result).asJson.noSpaces)

/** Winziger, rekursiver Parser/Evaluator für arithmetische Ausdrücke (+, -, *, /, Klammern, unäres Minus). Bewusst ohne externe Bibliothek - dieses Lernprojekt soll den Tool-Search-Flow zeigen, nicht
  * einen produktionsreifen Parser. Rein funktional: Jede `parseX`-Funktion bekommt die aktuelle Position explizit übergeben und liefert `(Wert, neue Position)` zurück - kein `var`, keine `while`-
  * Schleife (Projekt-Konvention, siehe `DisableSyntax` in `AGENTS.md`/`.scalafix.conf`).
  */
object ExpressionParser:
  def evaluate(expr: String): Double =
    val chars = expr.filterNot(_.isWhitespace)

    def peek(pos: Int): Option[Char] = if pos < chars.length then Some(chars(pos)) else None

    @annotation.tailrec
    def parseNumber(pos: Int, start: Int): (Double, Int) =
      peek(pos) match
        case Some(c) if c.isDigit || c == '.' => parseNumber(pos + 1, start)
        case _ if pos == start                => throw new IllegalArgumentException(s"Zahl erwartet an Position $pos in '$expr'")
        case _                                => (chars.substring(start, pos).toDouble, pos)

    def parseFactor(pos: Int): (Double, Int) =
      peek(pos) match
        case Some('(') =>
          val (value, afterExpr) = parseExpr(pos + 1)
          if !peek(afterExpr).contains(')') then throw new IllegalArgumentException(s"Erwartete ')' an Position $afterExpr in '$expr'")
          (value, afterExpr + 1)
        case Some('-') =>
          val (value, next) = parseFactor(pos + 1)
          (-value, next)
        case Some('+') => parseFactor(pos + 1)
        case _         => parseNumber(pos, pos)

    def parseBinaryLevel(pos: Int, next: Int => (Double, Int), ops: Map[Char, (Double, Double) => Double]): (Double, Int) =
      @annotation.tailrec
      def continue(value: Double, pos: Int): (Double, Int) =
        peek(pos) match
          case Some(op) if ops.contains(op) =>
            val (rhs, afterRhs) = next(pos + 1)
            continue(ops(op)(value, rhs), afterRhs)
          case _                            => (value, pos)
      val (first, afterFirst)                              = next(pos)
      continue(first, afterFirst)

    def parseTerm(pos: Int): (Double, Int) = parseBinaryLevel(pos, parseFactor, Map('*' -> (_ * _), '/' -> (_ / _)))
    def parseExpr(pos: Int): (Double, Int) = parseBinaryLevel(pos, parseTerm, Map('+' -> (_ + _), '-' -> (_ - _)))

    val (result, finalPos) = parseExpr(0)
    if finalPos != chars.length then throw new IllegalArgumentException(s"Unerwartetes Zeichen an Position $finalPos in '$expr'")
    result

/** Tool 3: `roll_dice` - würfelt `count`-mal einen Würfel mit `sides` Seiten und liefert die Einzelwürfe sowie die Summe.
  */
case class RollDiceInput(
    @description("Anzahl der Seiten des Würfels, z. B. 6 oder 20.")
    sides: Int,
    @description("Anzahl der Würfe.")
    count: Int,
) derives ConfiguredCodec, Schema
case class RollDiceResult(sides: Int, count: Int, rolls: List[Int], sum: Int) derives ConfiguredCodec

object RollDiceTool
    extends JsonTool[RollDiceInput](
      name = "roll_dice",
      description = "Würfelt 'count'-mal einen Würfel mit 'sides' Seiten und liefert die Einzelwürfe sowie die Summe.",
    ):

  protected def run(input: RollDiceInput): ToolCallResult =
    if input.sides < 2 || input.count < 1 then ToolCallResult.error("sides muss >= 2 und count muss >= 1 sein.")
    else
      val rolls = List.fill(input.count)(Random.nextInt(input.sides) + 1)
      ToolCallResult(RollDiceResult(sides = input.sides, count = input.count, rolls = rolls, sum = rolls.sum).asJson.noSpaces)
