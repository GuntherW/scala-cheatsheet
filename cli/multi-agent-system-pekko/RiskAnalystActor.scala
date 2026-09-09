package agents

import upickle.default.*
import org.apache.pekko.actor.typed.Behavior
import AnthropicModels.*

/** Eingabeparameter, wie sie das Modell (passend zum `input_schema` von `CalculateTcoTool`) liefert.
  */
final private case class CalculateTcoInput(technology: String, team_size: Int) derives ReadWriter

/** Rückgabe des Tools - bewusst als eigenes Case-Class-Schema, damit die JSON-Struktur klar erkennbar bleibt.
  */
final private case class CalculateTcoResult(
    technology: String,
    team_size: Int,
    estimated_monthly_cost_eur: Int,
    note: String,
) derives ReadWriter

/** Definition und Ausführung des client-seitigen (custom) Tools `calculate_tco`. Unveraendert gegenueber `research_scala/RiskAnalyst.scala`
  *   - Tool-Definition und Handler-Funktion haben nichts mit dem Nebenläufigkeitsmodell (Aktor vs. direkter Aufruf) zu tun.
  */
private object CalculateTcoTool:

  val definition: ClientTool = ClientTool(
    name = "calculate_tco",
    description = "Berechnet eine grobe geschätzte Total Cost of Ownership (TCO) pro Monat für " +
      "eine Technologie, basierend auf der Teamgröße. HINWEIS: Dies ist eine Demo-Berechnung mit " +
      "Dummy-Zahlen, keine echte Kostenanalyse.",
    input_schema = InputSchema(
      properties = Map(
        "technology" -> PropertySchema(`type` = "string", description = "Name der zu bewertenden Technologie, z. B. 'Kubernetes'"),
        "team_size"  -> PropertySchema(`type` = "integer", description = "Anzahl der Teammitglieder, die die Technologie betreiben/nutzen"),
      ),
      required = List("technology", "team_size"),
    ),
  )

  /** Dummy-Implementierung des calculate_tco-Tools. Wird vom `AgentActor` auf dem `blocking-io-dispatcher` ausgeführt (Teil des `chatWithTool`-Loops in `AnthropicClient`).
    */
  def handler(rawInput: ujson.Value): String =
    val input          = read[CalculateTcoInput](rawInput)
    // Fest codierte Dummy-Formel - rein illustrativ.
    val monthlyCostEur = 350 * input.team_size + 500
    val result         = CalculateTcoResult(
      technology = input.technology,
      team_size = input.team_size,
      estimated_monthly_cost_eur = monthlyCostEur,
      note = "Demo-Berechnung mit Dummy-Zahlen, keine reale Kostenanalyse.",
    )
    write(result)

/** Worker 2: Risk-Analyst-Aktor.
  *
  * Fachlich identisch zu `RiskAnalyst` in `research_scala`: Nutzt ausschliesslich das client-seitige (custom) Tool `calculate_tco`, um den Tool-Use-Loop klar isoliert zu demonstrieren. Laeuft als
  * eigener, dauerhaft lebender Kind-Aktor unabhaengig und parallel zum Fact-Researcher-Aktor - beide Aktoren kennen sich nicht gegenseitig und teilen keinen Zustand.
  */
object RiskAnalystActor:

  private val SystemPrompt =
    """Du bist der Risk-Analyst in einem Multi-Agenten-System.
      |
      |Deine EINZIGE Aufgabe: Finde Risiken, Fallstricke, Kosten, Sicherheitsbedenken
      |und Nachteile der gegebenen Technologie oder Entscheidung.
      |
      |Regeln:
      |- Nenne konkrete Nachteile, versteckte Kosten, Betriebsrisiken, Sicherheits-
      |  und Compliance-Aspekte sowie mögliche Fehlannahmen.
      |- Nutze IMMER genau einmal das Tool calculate_tco, um eine grobe
      |  Kostenschätzung für ein Team von 5 Personen einzuholen, und baue das
      |  Ergebnis (als Hinweis auf eine Demo-Berechnung gekennzeichnet) in deinen
      |  Bericht ein.
      |- Nenne KEINE Vorteile oder positiven Argumente - das übernimmt ein anderer Agent.
      |- Sei präzise und strukturiere die Antwort in Stichpunkten.
      |- Antworte auf Deutsch.
      |""".stripMargin

  def apply(): Behavior[AgentProtocol.Command] =
    AgentActor(
      name = "Risk-Analyst",
      systemPrompt = SystemPrompt,
      clientTools = List(CalculateTcoTool.definition),
      toolHandlers = Map("calculate_tco" -> CalculateTcoTool.handler),
    )

  def prompt(topic: String): String =
    s"Analysiere Risiken, Fallstricke und Nachteile zu folgendem Thema:\n\n$topic"
