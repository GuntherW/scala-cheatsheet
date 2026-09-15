package agents

import io.circe.Json
import sttp.ai.claude.models.Tool

/** Basisklasse für alle Agenten im Multi-Agenten-System.
  *
  * Terminologie (wichtig für die CCAF-Zertifizierung):
  *
  *   - '''Agent''': Eine eigenständige Einheit, die einen LLM-Aufruf (Model Call) mit einem spezifischen System-Prompt (Rolle) kapselt und eine klar abgegrenzte Aufgabe löst.
  *   - '''System Prompt''': Instruktion, die dem Modell VOR der eigentlichen Aufgabe mitgegeben wird und dessen Rolle, Ton und Einschränkungen definiert.
  *   - '''Tool Use / Function Calling''': Fähigkeit des Modells, definierte externe Werkzeuge aufzurufen, um Informationen zu beschaffen oder Aktionen auszuführen, die es selbst nicht "weiß"/kann.
  *     Zwei Arten: * Server-seitig (z. B. `web_search`): wird komplett vom Anthropic-Server ausgeführt, ein einzelner Request genügt. * Client-seitig / custom (z. B. `calculate_tco`): das Modell
  *     liefert nur den Aufrufwunsch zurück (`stop_reason == "tool_use"`), WIR führen die Funktion aus und senden das Ergebnis zurück (Multi-Turn-Loop, siehe `AnthropicClient.chat`).
  *   - '''Orchestrator''': Die übergeordnete Steuerungslogik, die entscheidet, welche Agenten wann aufgerufen werden (sequentiell oder parallel) und wie deren Ergebnisse zusammengeführt werden.
  *   - '''Worker Agent''': Ein Agent, der Teilaufgaben eigenständig und (meist) parallel zu anderen Workern bearbeitet, ohne von deren Zwischenergebnissen zu wissen.
  *   - '''Synthesis / Aggregator Agent''': Ein Agent, der die Ausgaben mehrerer Worker-Agenten als Kontext erhält und daraus ein konsolidiertes Endergebnis erzeugt.
  */
abstract class Agent(
    val name: String,
    val systemPrompt: String,
    val useWebSearch: Boolean = false,
    val clientTools: List[Tool] = Nil,
    val toolHandlers: Map[String, Map[String, Json] => String] = Map.empty,
    val model: String = "vertex/claude-sonnet-5@eu",
):

  /** Führt den Model-Call aus (bei Bedarf als Multi-Turn Tool-Use-Loop - siehe `AnthropicClient.chat`). Eine einzige Methode deckt dabei sowohl server-seitige (`useWebSearch`) als auch client-seitige
    * (`clientTools`) Tools ab, einzeln oder gemischt.
    */
  protected def run(userMessage: String, maxTokens: Int = 2000): String = AnthropicClient.chat(
    caller = name,
    model = model,
    systemPrompt = systemPrompt,
    userMessage = userMessage,
    useWebSearch = useWebSearch,
    clientTools = clientTools,
    toolHandlers = toolHandlers,
    maxTokens = maxTokens,
  )
