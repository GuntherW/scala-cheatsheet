package agents

import upickle.default.*

/** JSON-Modelle für die Anthropic Messages API (`POST /v1/messages`).
  *
  * Nur die Felder, die wir tatsächlich brauchen, werden abgebildet - upickle ignoriert beim Lesen standardmäßig unbekannte JSON-Felder, daher genügt eine schlanke Teilmenge.
  *
  * Alle Case-Classes bekommen ihren `ReadWriter` einfach über `derives ReadWriter` (Scala-3-Feature) - das erspart die separaten, manuell deklarierten Codec-`given`s, die frühere
  * jsoniter-scala-Variante dieser Datei brauchte.
  *
  * Für Felder, deren JSON-Form je nach Nachricht mal ein einfacher String, mal eine Liste von Content-Blöcken ist (Anthropics `content`-Feld), nutzen wir direkt `ujson.Value` - upickles eigene,
  * dynamische JSON-AST-Repräsentation. Ein eigener `RawJson`-Wrapper (wie er für jsoniter-scala nötig war) entfällt dadurch komplett.
  */
object AnthropicModels:

  // ---- Request (einfacher, einzelner Model-Call - z. B. mit web_search) ----

  final case class ChatMessage(role: String, content: String) derives ReadWriter

  /** Beschreibung eines server-seitigen Tools, z. B. der Web-Suche. `type` ist ein Scala-Softkeyword und muss daher in Backticks stehen.
    */
  final case class WebSearchTool(`type`: String = "web_search_20250305", name: String = "web_search", max_uses: Int = 5) derives ReadWriter

  final case class ChatRequest(model: String, max_tokens: Int, system: Option[String], messages: List[ChatMessage], tools: Option[List[WebSearchTool]]) derives ReadWriter

  // ---- Request (Tool-Use-Loop mit einem client-seitigen/custom Tool) ----

  /** JSON-Schema-Beschreibung eines einzelnen Eingabeparameters für ein client-seitiges Tool.
    */
  final case class PropertySchema(`type`: String, description: String) derives ReadWriter

  /** JSON-Schema für die Eingabeparameter eines client-seitigen Tools (`input_schema`). Das Modell nutzt dieses Schema, um zu entscheiden, welche Parameter es beim Tool-Aufruf mitschickt.
    */
  final case class InputSchema(`type`: String = "object", properties: Map[String, PropertySchema], required: List[String]) derives ReadWriter

  /** Definition eines client-seitigen (custom) Tools. Im Gegensatz zu `WebSearchTool` hat dieses Tool keinen server-seitigen `type` - das Modell liefert nur den Aufrufwunsch zurück, die Ausführung
    * übernimmt unser eigener Code (siehe `AnthropicClient.chatWithTool`).
    */
  final case class ClientTool(name: String, description: String, input_schema: InputSchema) derives ReadWriter

  /** Eine einzelne Nachricht im Tool-Use-Loop. `content` ist bewusst `ujson.Value`, damit hier sowohl ein simpler String (erste Nutzeranfrage) als auch eine Liste von Content-Blöcken
    * (Assistant-Antwort mit `tool_use`, oder unser `tool_result`) eingebettet werden kann.
    */
  final case class LoopMessage(role: String, content: ujson.Value) derives ReadWriter

  final case class LoopChatRequest(
      model: String,
      max_tokens: Int,
      system: Option[String],
      messages: List[LoopMessage],
      tools: Option[List[ClientTool]],
  ) derives ReadWriter

  /** Unser Ergebnis eines client-seitigen Tool-Aufrufs, zurückgesendet an das Modell. `tool_use_id` verknüpft das Ergebnis eindeutig mit dem ursprünglichen `tool_use`-Block.
    */
  final case class ToolResultBlock(`type`: String = "tool_result", tool_use_id: String, content: String) derives ReadWriter

  // ---- Response ----

  /** Ein Content-Block der Antwort. Antworten können mehrere Blocktypen enthalten (`text`, `thinking`, `tool_use`, `server_tool_use`, `web_search_tool_result`, ...). Wir bilden hier absichtlich EIN
    * einziges, "lockeres" Case-Class-Schema für alle Blocktypen ab (statt eines polymorphen ADTs), da uns pro Blocktyp jeweils nur eine Teilmenge der Felder interessiert - nicht genutzte Felder
    * bleiben einfach `None`.
    */
  final case class ContentBlock(
      `type`: String,
      text: Option[String] = None,
      id: Option[String] = None,
      name: Option[String] = None,
      input: Option[ujson.Value] = None,
      thinking: Option[String] = None,
      signature: Option[String] = None,
  ) derives ReadWriter

  final case class ChatResponse(id: String, content: List[ContentBlock], stop_reason: Option[String]) derives ReadWriter
