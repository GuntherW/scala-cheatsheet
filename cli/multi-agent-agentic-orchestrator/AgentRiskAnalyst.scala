package agents

import io.circe.syntax.*

/** Worker 2: Risk-Analyst
  *
  * Aufgabe: Sucht gezielt nach Fallstricken, Kosten, Sicherheitsbedenken oder Nachteilen einer Technologie/Entscheidung. Läuft unabhängig und parallel zum Fact-Researcher - beide kennen die Ausgabe
  * des jeweils anderen NICHT (kein geteilter Zwischenzustand, dadurch unabhängige/ unvoreingenommene Einschätzung).
  *
  * Dieser Agent nutzt bewusst NUR ein client-seitiges (custom) Tool namens `calculate_tco` (Definition & Ausführung siehe `CalculateTcoTool.scala`), um dieses Konzept im Code klar isoliert zu zeigen -
  * im Gegensatz zum server-seitigen `web_search` des Fact-Researcher (siehe `AgentFactResearcher.scala`). Beim client-seitigen Tool liefert das Modell nur den Aufrufwunsch zurück (`stop_reason ==
  * "tool_use"`); WIR führen die Funktion lokal aus und senden das Ergebnis zurück (Multi-Turn-Loop, siehe `AnthropicClient.chatWithTool`). Die Rückgabe ist bewusst eine simple Dummy-Berechnung - es
  * geht hier nur darum, Tool-Definition (JSON-Schema) und Ausführung (Handler-Funktion) im Zusammenspiel zu zeigen.
  *
  * Hinweis: `web_search` wurde hier bewusst NICHT zusätzlich eingebunden, da das Mischen von server- und client-seitigen Tools bei diesem Router dazu führt, dass auch für `web_search` ein
  * `tool_result` erwartet wird (statt es automatisch serverseitig aufzulösen) - das würde dieses Beispiel unnötig verkomplizieren.
  *
  * '''Upstream Agent Optimisation''' (siehe README): Analog zu `AgentFactResearcher` liefert dieser Agent strukturierte Daten (`RiskReport`, siehe `RiskReport.scala`) statt Freitext - einzelne
  * Risiken inkl. Kategorie/Schweregrad/Mitigation statt Prosa. Der Tool-Use-Loop (`AnthropicClient.buildAgent`) unterstützt kein natives Structured Output parallel zu client-seitigen Tools, daher
  * verlangt der System-Prompt reines JSON als finale Textantwort; das Ergebnis wird anschließend lokal geparst (`JsonExtraction.parseLenient`).
  */
object AgentRiskAnalyst extends Agent(
      name = "Risk-Analyst",
      systemPrompt =
        """Du bist der Risk-Analyst in einem Multi-Agenten-System.
          |
          |Deine EINZIGE Aufgabe: Finde Risiken, Fallstricke, Kosten, Sicherheitsbedenken
          |und Nachteile der gegebenen Technologie oder Entscheidung.
          |
          |Regeln:
          |- Nenne konkrete Nachteile, versteckte Kosten, Betriebsrisiken, Sicherheits-
          |  und Compliance-Aspekte sowie mögliche Fehlannahmen.
          |- Nutze IMMER genau einmal das Tool calculate_tco, um eine grobe
          |  Kostenschätzung für ein Team von 5 Personen einzuholen.
          |- Nenne KEINE Vorteile oder positiven Argumente - das übernimmt ein anderer Agent.
          |- Antworte am Ende AUSSCHLIESSLICH mit validem JSON (nach dem Tool-Aufruf), keine
          |  Erklärtexte davor/danach, keine Markdown-Codefences, exakt in folgender Form:
          |  {"risks": [{"description": "...", "category": "Kosten|Sicherheit|Betrieb|Compliance|Sonstiges", "severity": 1-5, "mitigation": "... oder null"}], "tcoEstimate": "Ergebnis von calculate_tco als Hinweis auf Demo-Berechnung, oder null"}
          |- "severity" bewertet den Schweregrad (1=gering, 5=kritisch).
          |- Antworte auf Deutsch (nur die Textfelder, nicht die JSON-Schlüssel).
          |""".stripMargin,
      clientTools = Seq(CalculateTcoTool.agentTool),
    ):

  def analyze(topic: String): RiskReport =
    val outcome = runStructured[RisksOnly](
      userMessage = s"Analysiere Risiken, Fallstricke und Nachteile zu folgendem Thema:\n\n$topic",
      attemptedAction = s"Risikoanalyse zu '$topic' (inkl. calculate_tco)",
    )
    RiskReport(
      risks = outcome.parsed.map(_.risks).getOrElse(List.empty),
      tcoEstimate = outcome.parsed.flatMap(_.tcoEstimate),
      status = outcome.status,
      failureType = outcome.failureType,
      attemptedAction = outcome.attemptedAction,
      alternativeApproaches = outcome.alternativeApproaches,
      rawOutputSnippet = outcome.rawOutputSnippet,
    )

  /** Generische Beschreibung für Registry/Planner (siehe `AgentSpec.scala`). Keine Abhängigkeiten - kann daher parallel zum Fact-Researcher laufen.
    *
    * `execute` liefert das strukturierte `RiskReport` als kompaktes JSON (statt Freitext) an nachgelagerte Agenten weiter (siehe `AgentSynthesis.scala`).
    */
  def spec(topic: String): AgentSpec = AgentSpec(
    id = name,
    description = "Sucht gezielt nach Risiken, Kosten, Sicherheitsbedenken und Nachteilen der gegebenen Technologie/Entscheidung (nutzt calculate_tco). Nennt keine Vorteile.",
    execute = _ => analyze(topic).asJson.noSpaces,
  )
