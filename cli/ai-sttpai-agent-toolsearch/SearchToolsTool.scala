package agent

import io.circe.Json
import io.circe.syntax.*
import sttp.ai.claude.models.{PropertySchema, Tool, ToolInputSchema}

/** Definition und Ausführung des Meta-Tools `search_tools` - der Kern des Tool-Search-Paradigmas.
  *
  * Anders als `web_search` (server-seitig) oder `calculate_tco` (client-seitig, direkt nutzbar) ist `search_tools` selbst ein ganz normales client-seitiges Tool - es liefert aber keine fachliche
  * Antwort, sondern eine Liste von *weiteren* Tools (Name + Description), die daraufhin von `AnthropicClient.chat` in die `tools`-Liste des NÄCHSTEN Requests aufgenommen werden. Das Modell "entdeckt"
  * seine Werkzeuge also schrittweise, statt sie alle von Anfang an zu sehen.
  */
object SearchToolsTool:

  val Name = "search_tools"

  val definition: Tool.Custom = Tool(
    name = Name,
    description = "Durchsucht die verfügbare Tool-Bibliothek nach passenden Werkzeugen für eine Aufgabe. " +
      "Rufe dieses Tool IMMER zuerst auf, bevor du ein spezifisches Werkzeug (z. B. für Zeit, Rechnen oder " +
      "Würfeln) benutzt - du siehst diese Werkzeuge sonst nicht direkt. Gib eine kurze Beschreibung dessen, " +
      "was du tun möchtest, als 'query' an, z. B. 'aktuelle Uhrzeit' oder 'zwei Würfel werfen'.",
    inputSchema = ToolInputSchema.forObject(
      properties = Map(
        "query" -> PropertySchema.string("Kurze Beschreibung der benötigten Fähigkeit/Aufgabe, z. B. 'aktuelle Uhrzeit' oder 'Ausdruck berechnen'."),
      ),
      required = Some(List("query")),
    ),
  )

  /** Ergebnis eines Treffers, wie es dem Modell als JSON zurückgegeben wird - bewusst OHNE das komplette JSON-Schema, das braucht das Modell an dieser Stelle nicht (es bekommt das Schema automatisch
    * über die erweiterte `tools`-Liste des nächsten Requests, siehe `AnthropicClient.chat`).
    */
  private case class ToolSearchHit(name: String, description: String)
  private given io.circe.Encoder[ToolSearchHit] = io.circe.Encoder.forProduct2("name", "description")(h => (h.name, h.description))

  /** Führt die Suche aus und gibt sowohl den JSON-Text (für das `ToolResult`) als auch die Namen der gefundenen Tools zurück (letztere braucht `AnthropicClient.chat`, um die `tools`-Liste des
    * nächsten Requests zu erweitern).
    */
  def search(rawInput: Map[String, Json]): (String, List[String]) =
    val query = rawInput.get("query").flatMap(_.asString).getOrElse("")
    val hits  = ToolRegistry.search(query)
    if hits.isEmpty then
      (Json.obj("hits" -> Json.arr(), "note" -> "Keine passenden Tools gefunden.".asJson).noSpaces, Nil)
    else
      val hitJson = hits.map(h => ToolSearchHit(h.definition.name, h.definition.description))
      (Json.obj("hits" -> hitJson.asJson).noSpaces, hits.map(_.definition.name))
