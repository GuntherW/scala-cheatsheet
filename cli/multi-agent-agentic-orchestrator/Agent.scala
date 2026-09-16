package agents

import sttp.ai.core.agent.AgentTool
import sttp.shared.Identity

/** Basisklasse für alle Agenten im Multi-Agenten-System.
  *
  * Terminologie (wichtig für die CCAF-Zertifizierung):
  *
  *   - '''Agent''': Eine eigenständige Einheit, die einen LLM-Aufruf (Model Call) mit einem spezifischen System-Prompt (Rolle) kapselt und eine klar abgegrenzte Aufgabe löst.
  *   - '''System Prompt''': Instruktion, die dem Modell VOR der eigentlichen Aufgabe mitgegeben wird und dessen Rolle, Ton und Einschränkungen definiert.
  *   - '''Tool Use / Function Calling''': Fähigkeit des Modells, definierte externe Werkzeuge aufzurufen, um Informationen zu beschaffen oder Aktionen auszuführen, die es selbst nicht "weiß"/kann.
  *     Zwei Arten: * Server-seitig (z. B. `web_search`): wird komplett vom Anthropic-Server ausgeführt, ein einzelner Request genügt. * Client-seitig / custom (z. B. `calculate_tco`): das Modell
  *     liefert nur den Aufrufwunsch zurück (`stop_reason == "tool_use"`), WIR führen die Funktion aus und senden das Ergebnis zurück (Multi-Turn-Loop).
  *   - '''Orchestrator''': Die übergeordnete Steuerungslogik, die entscheidet, welche Agenten wann aufgerufen werden (sequentiell oder parallel) und wie deren Ergebnisse zusammengeführt werden.
  *   - '''Worker Agent''': Ein Agent, der Teilaufgaben eigenständig und (meist) parallel zu anderen Workern bearbeitet, ohne von deren Zwischenergebnissen zu wissen.
  *   - '''Synthesis / Aggregator Agent''': Ein Agent, der die Ausgaben mehrerer Worker-Agenten als Kontext erhält und daraus ein konsolidiertes Endergebnis erzeugt.
  *   - '''Interceptor''': sttp-ai-Middleware um den Agent-Loop (Logging, Usage-Tracking, Budgets - siehe `Interceptors.scala`), onion-style um jede Iteration/jeden LLM-Call/Tool-Aufruf gelegt (siehe
  *     `AnthropicClient.buildAgent`).
  *
  * Der Model-Call selbst läuft immer über `AnthropicClient.buildAgent`, das intern - für `Agent.scala` transparent - entweder den generischen, client-seitigen Tool-Use-Loop von sttp-ai
  * (`ClaudeAgent.synchronous`) oder einen bewusst simplen Einzel-Request-Pfad für das server-seitige `web_search`-Tool wählt (siehe `AnthropicClient.buildAgent`/`webSearchAgent` für die Begründung) -
  * statt des früheren, direkt in diesem Projekt handgeschriebenen Tool-Use-Loops -, inkl. Logging-/Usage-/Budget-Interceptoren.
  */
abstract class Agent(
    val name: String,
    val systemPrompt: String,
    val useWebSearch: Boolean = false,
    val clientTools: Seq[AgentTool[Identity, ?]] = Seq.empty,
    val model: String = "vertex/claude-sonnet-5@eu",
):

  require(
    !(useWebSearch && clientTools.nonEmpty),
    "web_search laesst sich nicht mit client-seitigen Tools kombinieren (siehe README, Abschnitt zu Tool-Typen) - " +
      "der Einzel-Request-Pfad fuer web_search bietet dafuer keinen Tool-Use-Loop an.",
  )

  /** Führt den Model-Call über `AnthropicClient.buildAgent` aus - EIN einheitlicher Aufruf für beide Tool-Typen (server-/client-seitig, bewusst exklusiv, siehe `require` oben); welcher der beiden
    * Pfade intern läuft, entscheidet `AnthropicClient.buildAgent` selbst anhand von `useWebSearch`.
    *
    * Bei einem "unsauberen" Ende (z. B. Budget-Interceptor greift, `maxTokens` erreicht, oder - nur beim Tool-Use-Loop - maximale Iterationen erreicht, bevor eine finale Text-Antwort ohne Tool-Aufruf
    * kam) wird die bestmögliche verfügbare Antwort (`AgentFailure.rawAnswer`) zurückgegeben statt hart zu scheitern - das entspricht dem bisherigen, bewusst nachsichtigen Verhalten dieses
    * Lernprojekts (siehe `AgentConfig`-Systemprompt: "IF THIS IS THE LAST ALLOWED ITERATION, provide your final answer, even if the result is partial").
    */
  protected def run(userMessage: String, maxTokens: Int = 2000): String =
    val agent = AnthropicClient.buildAgent(
      caller = name,
      model = model,
      systemPrompt = systemPrompt,
      tools = clientTools,
      useWebSearch = useWebSearch,
      maxTokens = Some(maxTokens),
    )
    agent.run(userMessage)(AnthropicClient.backend).finalAnswer match
      case Right(answer) => answer
      case Left(failure) =>
        println(s"[LLM:$name/WARN] Kein regulärer Abschluss des Model-Calls (${failure.getClass.getSimpleName}) - nutze bestmögliche Antwort.")
        failure.rawAnswer
