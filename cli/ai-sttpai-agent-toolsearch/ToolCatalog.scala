package agent

import io.circe.Json
import io.circe.derivation.ConfiguredCodec
import io.circe.syntax.*
import sttp.ai.claude.models.Tool
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description

/** Ergebnis der Ausführung EINES Tool-Aufrufs: der Text, der als `ToolResult` ans Modell zurückgeht, ob es sich dabei um einen Fehlerfall handelt (`isError` - setzt Claudes offizielles
  * `tool_result.is_error`-Feld, siehe `AnthropicClient.executeToolUse`), sowie optional weitere Tools, die dadurch ab dem nächsten Turn sichtbar werden sollen.
  *
  * `enables` ist der GESAMTE Mechanismus hinter dem Tool-Search-Paradigma - es gibt dafür keinen Sonderfall im `AnthropicClient`-Loop: Jeder Tool-Aufruf kann grundsätzlich weitere Tools freischalten,
  * bei den drei "fachlichen" Tools (`get_current_time`, `calculator`, `roll_dice`) ist diese Liste einfach immer leer. Nur `search_tools` befüllt sie - ist aber selbst ein ganz normales
  * `RegisteredTool` wie jedes andere auch.
  */
case class ToolCallResult(output: String, isError: Boolean = false, enables: List[Tool.CustomRaw] = Nil)

object ToolCallResult:
  /** Baut ein einheitliches Fehler-`ToolCallResult`: `{"error": "<message>"}` als `output`, `isError = true`. Zentraler Baustein für alle Fehlerpfade (Decode-Fehler, Validierung, unbekanntes Tool,
    * ...) in `Tools.scala` und `AnthropicClient.scala`, damit das Fehler-JSON-Format an EINER Stelle gepflegt wird statt an jeder Fehlerstelle einzeln dupliziert zu sein.
    */
  def error(message: String): ToolCallResult = ToolCallResult(output = Json.obj("error" -> message.asJson).noSpaces, isError = true)

/** Ein im System registrierbares Tool: JSON-Schema fürs Modell (`definition`), Suchbegriffe für `search_tools` (`keywords`) und die eigentliche Ausführung (`handler`).
  */
case class RegisteredTool(definition: Tool.CustomRaw, keywords: List[String], handler: Map[String, Json] => ToolCallResult)

/** Zentrale, einzige Anlaufstelle für alle Tools des Agenten - INKLUSIVE des Meta-Tools `search_tools` selbst. Sowohl die drei "fachlichen" Tools als auch `search_tools` sind `RegisteredTool`s und
  * werden vom `AnthropicClient` über denselben `find`/`handler`-Mechanismus aufgerufen. Das vereinheitlicht den Tool-Search-Ablauf zu einem einzigen generischen Muster ("ein Tool-Aufruf liefert einen
  * Ergebnistext und ggf. neu freigeschaltete Tools"), statt ihn im Client per Namens-Sonderfall ("ist das jetzt search_tools oder ein echtes Tool?") zu behandeln.
  */
object ToolCatalog:

  /** Die drei fachlichen Tools - ihre Handler liefern nie zusätzliche `enables`, da sie selbst keine weiteren Tools freischalten. */
  private val realTools: List[RegisteredTool] = List(
    RegisteredTool(
      definition = CurrentTimeTool.definition,
      keywords = List("zeit", "uhrzeit", "datum", "time", "date", "clock", "wie spät", "timezone", "zeitzone"),
      handler = CurrentTimeTool.handler,
    ),
    RegisteredTool(
      definition = CalculatorTool.definition,
      keywords = List("rechnen", "berechne", "berechnung", "arithmetik", "math", "calculate", "plus", "minus", "mal", "geteilt", "summe", "addieren"),
      handler = CalculatorTool.handler,
    ),
    RegisteredTool(
      definition = RollDiceTool.definition,
      keywords = List("würfel", "wuerfel", "würfeln", "dice", "roll", "zufall", "random", "zufallszahl"),
      handler = RollDiceTool.handler,
    ),
  )

  // Name/Description/Keywords ändern sich nie zur Laufzeit - der durchsuchbare Text pro Tool wird deshalb einmalig statt bei jeder Suche neu gebaut.
  private val searchableTextByTool: Map[RegisteredTool, String] =
    realTools.map(t => t -> (t.definition.name :: t.definition.description :: t.keywords).mkString(" ").toLowerCase).toMap

  /** Simple Keyword-/Substring-Suche über Name, Description und `keywords` der fachlichen Tools (case-insensitive). Bewusst simpel gehalten - in einem echten System mit vielen Tools stünde hier
    *   z. B. eine Vektor-/Embedding-Suche.
    *
    * WICHTIG: `\W` ist in Java/Scala standardmäßig NUR ASCII-basiert - Umlaute wie 'ü' gelten dann fälschlich als Trenner ("Würfel" -> "w", "rfel"). Das `(?U)`-Flag schaltet Unicode-bewusstes
    * Wort-Matching ein. Terme mit weniger als 3 Zeichen werden verworfen, da sie sonst per Substring-Suche fast überall zufällig "treffen" (z. B. ein einzelnes "w") und ungewollt zusätzliche,
    * eigentlich irrelevante Tools freischalten würden.
    */
  private def searchRealTools(query: String): List[RegisteredTool] =
    val terms = query.toLowerCase.split("(?U)\\W+").filter(_.length >= 3)
    if terms.isEmpty then Nil else realTools.filter(t => terms.exists(searchableTextByTool(t).contains))

  /** Eingabe von `search_tools` - wie die Input-Typen der fachlichen Tools (`Tools.scala`) per Tapir `Schema` annotiert, damit auch `search_tools` sein JSON-Schema aus der Case Class ableitet statt
    * es manuell aufzuschreiben.
    */
  private case class SearchToolsInput(
      @description("Kurze Beschreibung der benötigten Fähigkeit/Aufgabe, z. B. 'aktuelle Uhrzeit' oder 'Ausdruck berechnen'.")
      query: String,
  ) derives ConfiguredCodec, Schema

  /** Das Meta-Tool `search_tools`: durchsucht die fachlichen Tools per Keyword-Suche und liefert die Treffer sowohl als JSON-Text (fürs Modell, in `output`) als auch als freizuschaltende
    * Tool-Definitionen (`enables`) - das ist der einzige Ort im gesamten Projekt, an dem "Tool-Search" tatsächlich passiert. Nutzt wie die fachlichen Tools `JsonTool` als Basisklasse - dadurch
    * bekommt auch `query` automatisch eine korrekte Decode-Fehlerbehandlung (vorher wurde ein fehlendes/falsch typisiertes `query`-Feld stillschweigend zu einem leeren String).
    */
  private object SearchToolsTool
      extends JsonTool[SearchToolsInput](
        name = "search_tools",
        description = "Durchsucht die verfügbare Tool-Bibliothek nach passenden Werkzeugen für eine Aufgabe. " +
          "Rufe dieses Tool IMMER zuerst auf, bevor du ein spezifisches Werkzeug (z. B. für Zeit, Rechnen oder " +
          "Würfeln) benutzt - du siehst diese Werkzeuge sonst nicht direkt. Gib eine kurze Beschreibung dessen, " +
          "was du tun möchtest, als 'query' an, z. B. 'aktuelle Uhrzeit' oder 'zwei Würfel werfen'.",
      ):

    protected def run(input: SearchToolsInput): ToolCallResult =
      val hits   = searchRealTools(input.query)
      val output =
        if hits.isEmpty then Json.obj("hits" -> Json.arr(), "note" -> "Keine passenden Tools gefunden.".asJson).noSpaces
        else
          val hitsJson = hits.map(h => Json.obj("name" -> h.definition.name.asJson, "description" -> h.definition.description.asJson))
          Json.obj("hits" -> hitsJson.asJson).noSpaces
      ToolCallResult(output = output, enables = hits.map(_.definition))

  private val searchTool: RegisteredTool = RegisteredTool(definition = SearchToolsTool.definition, keywords = Nil, handler = SearchToolsTool.handler)

  /** ALLE registrierten Tools, `search_tools` inklusive - der `AnthropicClient` unterscheidet beim Aufruf nicht zwischen ihnen. */
  val all: List[RegisteredTool] = searchTool :: realTools

  private val byName: Map[String, RegisteredTool] = all.map(t => t.definition.name -> t).toMap

  def find(name: String): Option[RegisteredTool] = byName.get(name)

  /** Das einzige Tool, das der Agent zu Beginn der Konversation kennt (siehe `AnthropicClient.chat`). */
  val initiallyVisible: Tool.CustomRaw = searchTool.definition
