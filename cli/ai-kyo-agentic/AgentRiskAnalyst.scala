package agents

import kyo.*

/** Worker 2: Risk-Analyst
  *
  * Aufgabe: Sucht gezielt nach Fallstricken, Kosten, Sicherheitsbedenken oder Nachteilen einer Technologie/Entscheidung. Läuft unabhängig und parallel zum Fact-Researcher - beide kennen die Ausgabe
  * des jeweils anderen NICHT (kein geteilter Zwischenzustand, dadurch unabhängige/unvoreingenommene Einschätzung).
  *
  * Nutzt bewusst NUR ein client-seitiges (custom) Tool namens `calculate_tco` (`CalculateTcoTool.scala`), um dieses Konzept im Code klar isoliert vom server-seitigen `web_search` des Fact-Researcher
  * zu zeigen (siehe `AgentFactResearcher.scala`/`WebSearchClient.scala`). Anders als im sttp-ai-Original lässt sich `calculate_tco` hier problemlos MIT einem typisierten Ergebnis
  * (`AI.gen[RisksOnly]`, siehe `Agents.runStructured`) kombinieren - kyo-ai erzwingt das Ergebnis-Schema über einen eigenen "Result-Tool"-Mechanismus, der neben gewöhnlichen Tools laufen kann (siehe
  * kyo-ai-README, Abschnitt "Typed results"/"Tools and the automatic loop") - das manuelle JSON-Prompting+Parsing des Originals entfällt daher komplett.
  */
object AgentRiskAnalyst:

  val name = "Risk-Analyst"

  private val systemPrompt =
    p"""Du bist der Risk-Analyst in einem Multi-Agenten-System.
        Deine EINZIGE Aufgabe: Finde Risiken, Fallstricke, Kosten, Sicherheitsbedenken
        und Nachteile der gegebenen Technologie oder Entscheidung.
        Regeln:
        - Nenne konkrete Nachteile, versteckte Kosten, Betriebsrisiken, Sicherheits-
        und Compliance-Aspekte sowie mögliche Fehlannahmen.
        - Nutze IMMER genau einmal das Tool calculate_tco, um eine grobe
        Kostenschätzung für ein Team von 5 Personen einzuholen.
        - Nenne KEINE Vorteile oder positiven Argumente - das übernimmt ein anderer Agent.
        - category ist eine von: Kosten|Sicherheit|Betrieb|Compliance|Sonstiges.
        - severity bewertet den Schweregrad (1=gering, 5=kritisch).
        - tcoEstimate ist das Ergebnis von calculate_tco als Hinweis auf eine Demo-Berechnung.
        - Antworte auf Deutsch (nur die Textfelder)."""

  def analyze(topic: String): RiskReport < (Async & Sync) =
    Agents
      .runStructured[RisksOnly](
        caller = name,
        systemPrompt = systemPrompt,
        userMessage = s"Analysiere Risiken, Fallstricke und Nachteile zu folgendem Thema:\n\n$topic",
        attemptedAction = s"Risikoanalyse zu '$topic' (inkl. calculate_tco)",
        tools = Seq(CalculateTcoTool.tool),
      )
      .map { outcome =>
        RiskReport(
          risks = outcome.parsed.map(_.risks).getOrElse(List.empty),
          tcoEstimate = outcome.parsed.flatMap(_.tcoEstimate),
          status = outcome.status,
          failureType = outcome.failureType,
          attemptedAction = outcome.attemptedAction,
          alternativeApproaches = outcome.alternativeApproaches,
          rawOutputSnippet = outcome.rawOutputSnippet,
        )
      }

  /** Generische Beschreibung für Registry/Planner (siehe `AgentSpec.scala`). Keine Abhängigkeiten - kann daher parallel zum Fact-Researcher laufen. `execute` liefert das strukturierte `RiskReport`
    * als kompaktes JSON (statt Freitext) an nachgelagerte Agenten weiter (siehe `AgentSynthesis.scala`).
    */
  def spec(topic: String): AgentSpec = AgentSpec(
    id = name,
    description = "Sucht gezielt nach Risiken, Kosten, Sicherheitsbedenken und Nachteilen der gegebenen Technologie/Entscheidung (nutzt calculate_tco). Nennt keine Vorteile.",
    execute = _ => analyze(topic).map(Json.encode(_)),
  )
