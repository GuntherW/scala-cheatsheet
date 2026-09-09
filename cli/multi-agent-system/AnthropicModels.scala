package agents

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

/** JSON-Modelle für die Anthropic Messages API (`POST /v1/messages`).
  *
  * Nur die Felder, die wir tatsächlich brauchen, werden abgebildet -
  * jsoniter-scala überspringt standardmäßig unbekannte JSON-Felder
  * (`skipUnexpectedFields = true`), daher genügt eine schlanke Teilmenge.
  */
object AnthropicModels:

  /** Wrapper für "rohes" JSON, das unverändert (ohne erneutes
    * Escaping/Parsen) in ein umgebendes JSON-Dokument eingebettet werden
    * soll. Wird gebraucht, weil Anthropic's `content`-Feld je nach
    * Nachricht entweder ein einfacher String ODER eine Liste von
    * Content-Blöcken unterschiedlichster Form sein kann - etwas, das sich
    * mit rein statisch typisierten jsoniter-scala-Case-Classes nicht
    * elegant abbilden lässt. `RawJson` umgeht das, indem es die Bytes
    * 1:1 durchreicht (`writeRawVal` / `readRawValAsBytes`).
    */
  final case class RawJson(bytes: Array[Byte])

  given rawJsonCodec: JsonValueCodec[RawJson] with
    def decodeValue(in: JsonReader, default: RawJson): RawJson = RawJson(in.readRawValAsBytes())
    def encodeValue(x: RawJson, out: JsonWriter): Unit = out.writeRawVal(x.bytes)
    def nullValue: RawJson = RawJson(Array.empty)

  /** jsoniter-scala stellt für einzelne Primitive wie `String` keinen
    * global nutzbaren `given JsonValueCodec[String]` bereit (die
    * Case-Class-Makros erzeugen den Umgang mit Strings intern, ohne dafür
    * eine öffentliche Instanz zu benötigen). Für `rawJsonString` unten
    * brauchen wir aber genau das - daher hier eine minimale eigene
    * Implementierung auf Basis von `JsonWriter.writeVal`/`JsonReader.readString`.
    */
  given stringCodec: JsonValueCodec[String] with
    def decodeValue(in: JsonReader, default: String): String = in.readString(default)
    def encodeValue(x: String, out: JsonWriter): Unit = out.writeVal(x)
    def nullValue: String = null

  /** Baut ein `RawJson` aus einem gewöhnlichen Scala-String (inkl. korrektem
    * JSON-Escaping der Anführungszeichen etc.).
    */
  def rawJsonString(s: String): RawJson = RawJson(writeToArray(s)(using stringCodec))

  // ---- Request (einfacher, einzelner Model-Call - z. B. mit web_search) ----

  final case class ChatMessage(role: String, content: String)

  /** Beschreibung eines server-seitigen Tools, z. B. der Web-Suche.
    * `type` ist ein Scala-Softkeyword und muss daher in Backticks stehen.
    */
  final case class WebSearchTool(`type`: String = "web_search_20250305", name: String = "web_search", max_uses: Int = 5)

  final case class ChatRequest(
      model: String,
      max_tokens: Int,
      system: Option[String],
      messages: List[ChatMessage],
      tools: Option[List[WebSearchTool]],
  )

  // `transientDefault = false` ist wichtig: sonst würden Felder, die zufällig
  // ihrem Default-Wert entsprechen (z. B. `type = "web_search_20250305"`),
  // beim Serialisieren stillschweigend weggelassen - mit fatalen Folgen für
  // die Anthropic API (leeres Tool-Objekt statt gültiger Tool-Definition).
  given chatRequestCodec: JsonValueCodec[ChatRequest] =
    JsonCodecMaker.make(CodecMakerConfig.withTransientDefault(false))

  // ---- Request (Tool-Use-Loop mit einem client-seitigen/custom Tool) ----

  /** JSON-Schema-Beschreibung eines einzelnen Eingabeparameters für ein
    * client-seitiges Tool.
    */
  final case class PropertySchema(`type`: String, description: String)

  /** JSON-Schema für die Eingabeparameter eines client-seitigen Tools
    * (`input_schema`). Das Modell nutzt dieses Schema, um zu entscheiden,
    * welche Parameter es beim Tool-Aufruf mitschickt.
    */
  final case class InputSchema(
      `type`: String = "object",
      properties: Map[String, PropertySchema],
      required: List[String],
  )

  /** Definition eines client-seitigen (custom) Tools. Im Gegensatz zu
    * `WebSearchTool` hat dieses Tool keinen server-seitigen `type` -
    * das Modell liefert nur den Aufrufwunsch zurück, die Ausführung
    * übernimmt unser eigener Code (siehe `AnthropicClient.chatWithTool`).
    */
  final case class ClientTool(name: String, description: String, input_schema: InputSchema)

  /** Eine einzelne Nachricht im Tool-Use-Loop. `content` ist bewusst
    * `RawJson`, damit hier sowohl ein simpler String (erste Nutzeranfrage)
    * als auch eine Liste von Content-Blöcken (Assistant-Antwort mit
    * `tool_use`, oder unser `tool_result`) eingebettet werden kann.
    */
  final case class LoopMessage(role: String, content: RawJson)

  final case class LoopChatRequest(
      model: String,
      max_tokens: Int,
      system: Option[String],
      messages: List[LoopMessage],
      tools: Option[List[ClientTool]],
  )

  given loopMessageListCodec: JsonValueCodec[List[LoopMessage]] =
    JsonCodecMaker.make(CodecMakerConfig.withTransientDefault(false))
  given loopChatRequestCodec: JsonValueCodec[LoopChatRequest] =
    JsonCodecMaker.make(CodecMakerConfig.withTransientDefault(false))

  /** Unser Ergebnis eines client-seitigen Tool-Aufrufs, zurückgesendet an
    * das Modell. `tool_use_id` verknüpft das Ergebnis eindeutig mit dem
    * ursprünglichen `tool_use`-Block.
    */
  final case class ToolResultBlock(`type`: String = "tool_result", tool_use_id: String, content: String)

  given toolResultBlockListCodec: JsonValueCodec[List[ToolResultBlock]] =
    JsonCodecMaker.make(CodecMakerConfig.withTransientDefault(false))

  // ---- Response ----

  /** Ein Content-Block der Antwort. Antworten können mehrere Blocktypen
    * enthalten (`text`, `thinking`, `tool_use`, `server_tool_use`,
    * `web_search_tool_result`, ...). Wir bilden hier absichtlich EIN
    * einziges, "lockeres" Case-Class-Schema für alle Blocktypen ab (statt
    * eines polymorphen ADTs), da uns pro Blocktyp jeweils nur eine
    * Teilmenge der Felder interessiert - nicht genutzte Felder bleiben
    * einfach `None`.
    */
  final case class ContentBlock(
      `type`: String,
      text: Option[String] = None,
      id: Option[String] = None,
      name: Option[String] = None,
      input: Option[RawJson] = None,
      thinking: Option[String] = None,
      signature: Option[String] = None,
  )

  final case class ChatResponse(
      id: String,
      content: List[ContentBlock],
      stop_reason: Option[String],
  )

  given contentBlockCodec: JsonValueCodec[ContentBlock] = JsonCodecMaker.make
  given contentBlockListCodec: JsonValueCodec[List[ContentBlock]] = JsonCodecMaker.make
  given chatResponseCodec: JsonValueCodec[ChatResponse] = JsonCodecMaker.make

