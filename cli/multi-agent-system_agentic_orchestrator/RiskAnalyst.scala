package agents

import io.circe.{Codec, Json}
import io.circe.syntax.*
import sttp.ai.claude.models.{PropertySchema, Tool, ToolInputSchema}

/** Eingabeparameter, wie sie das Modell (passend zum `inputSchema` von `CalculateTcoTool.definition`) liefert.
  */
case class CalculateTcoInput(technology: String, team_size: Int) derives Codec.AsObject

/** Rückgabe des Tools - bewusst als eigenes Case-Class-Schema, damit die JSON-Struktur klar erkennbar bleibt.
  */
case class CalculateTcoResult(technology: String, team_size: Int, estimated_monthly_cost_eur: Int, note: String) derives Codec.AsObject

/** Definition und Ausführung des client-seitigen (custom) Tools `calculate_tco`. Als eigenes Objekt VOR `RiskAnalyst` definiert, damit es beim Aufbau von `RiskAnalyst extends Agent(...)` bereits
  * vollständig initialisiert zur Verfügung steht (kein Vorwärtsverweis auf `RiskAnalyst` selbst nötig).
  */
object CalculateTcoTool:

  /** Client-seitiges (custom) Tool: Definition per JSON-Schema (`ToolInputSchema`/`PropertySchema` aus sttp-ai). Das Modell entscheidet selbst, WANN es dieses Tool mit welchen Parametern aufruft -
    * die eigentliche Ausführung übernimmt `handler` unten.
    */
  val definition: Tool.Custom = Tool(
    name = "calculate_tco",
    description = "Berechnet eine grobe geschätzte Total Cost of Ownership (TCO) pro Monat für " +
      "eine Technologie, basierend auf der Teamgröße. HINWEIS: Dies ist eine Demo-Berechnung mit " +
      "Dummy-Zahlen, keine echte Kostenanalyse.",
    inputSchema = ToolInputSchema.forObject(
      properties = Map(
        "technology" -> PropertySchema.string("Name der zu bewertenden Technologie, z. B. 'Kubernetes'"),
        "team_size"  -> PropertySchema.integer("Anzahl der Teammitglieder, die die Technologie betreiben/nutzen"),
      ),
      required = Some(List("technology", "team_size")),
    ),
  )

  /** Dummy-Implementierung des calculate_tco-Tools.
    *
    * Führt keine echte Kostenanalyse durch, sondern demonstriert nur, wie ein client-seitiges Tool lokal ausgeführt und dessen Ergebnis als JSON-String an das Modell zurückgegeben wird.
    */
  def handler(rawInput: Map[String, Json]): String =
    val input          = Json.fromFields(rawInput).as[CalculateTcoInput]
      .getOrElse(throw new RuntimeException(s"Konnte calculate_tco-Eingabe nicht parsen: $rawInput"))
    // Fest codierte Dummy-Formel - rein illustrativ.
    val monthlyCostEur = 350 * input.team_size + 500
    val result         = CalculateTcoResult(
      technology = input.technology,
      team_size = input.team_size,
      estimated_monthly_cost_eur = monthlyCostEur,
      note = "Demo-Berechnung mit Dummy-Zahlen, keine reale Kostenanalyse.",
    )
    result.asJson.noSpaces

/** Worker 2: Risk-Analyst
  *
  * Aufgabe: Sucht gezielt nach Fallstricken, Kosten, Sicherheitsbedenken oder Nachteilen einer Technologie/Entscheidung. Läuft unabhängig und parallel zum Fact-Researcher - beide kennen die Ausgabe
  * des jeweils anderen NICHT (kein geteilter Zwischenzustand, dadurch unabhängige/ unvoreingenommene Einschätzung).
  *
  * Dieser Agent nutzt bewusst NUR ein client-seitiges (custom) Tool namens `calculate_tco`, um dieses Konzept im Code klar isoliert zu zeigen - im Gegensatz zum server-seitigen `web_search` des
  * Fact-Researcher (siehe `FactResearcher.scala`). Beim client-seitigen Tool liefert das Modell nur den Aufrufwunsch zurück (`stop_reason == "tool_use"`); WIR führen die Funktion lokal aus und senden
  * das Ergebnis zurück (Multi-Turn-Loop, siehe `AnthropicClient.chatWithTool`). Die Rückgabe ist bewusst eine simple Dummy-Berechnung - es geht hier nur darum, Tool-Definition (JSON-Schema) und
  * Ausführung (Handler-Funktion) im Zusammenspiel zu zeigen.
  *
  * Hinweis: `web_search` wurde hier bewusst NICHT zusätzlich eingebunden, da das Mischen von server- und client-seitigen Tools bei diesem Router dazu führt, dass auch für `web_search` ein
  * `tool_result` erwartet wird (statt es automatisch serverseitig aufzulösen) - das würde dieses Beispiel unnötig verkomplizieren.
  */
object RiskAnalyst extends Agent(
      name = "Risk-Analyst",
      systemPrompt = """Du bist der Risk-Analyst in einem Multi-Agenten-System.
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
                       |""".stripMargin,
      clientTools = List(CalculateTcoTool.definition),
      toolHandlers = Map(CalculateTcoTool.definition.name -> CalculateTcoTool.handler),
    ):

  def analyze(topic: String): String =
    run(s"Analysiere Risiken, Fallstricke und Nachteile zu folgendem Thema:\n\n$topic")
