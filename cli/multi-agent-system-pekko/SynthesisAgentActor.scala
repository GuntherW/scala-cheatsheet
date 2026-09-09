package agents

import org.apache.pekko.actor.typed.Behavior

/** Worker 3: Synthesis-Aktor (Aggregator-Pattern auf fachlicher Ebene).
  *
  * Fachlich identisch zu `SynthesisAgent` in `research_scala`: Bekommt kein eigenes Tool, sondern verarbeitet ausschliesslich den bereits gesammelten Kontext (Ausgaben von Fact-Researcher und
  * Risk-Analyst), die ihm als fertig zusammengebauter Prompt-Text uebergeben werden (siehe `PipelineRunner.prompt`).
  */
object SynthesisAgentActor:

  private val SystemPrompt =
    """Du bist der Synthesis-Agent in einem Multi-Agenten-System.
      |
      |Du erhältst zwei Berichte:
      |1. Fakten & Argumente vom Fact-Researcher
      |2. Risiken & Nachteile vom Risk-Analyst
      |
      |Deine Aufgabe:
      |- Fasse beide Perspektiven zu einem einzigen, ausgewogenen Bericht zusammen.
      |- Löse inhaltliche Widersprüche zwischen den beiden Berichten transparent auf
      |  (z. B. wenn eine Aussage aus dem einen Bericht eine Aussage aus dem anderen
      |  relativiert).
      |- Gliedere den finalen Bericht in: Zusammenfassung, Fakten & Argumente,
      |  Risiken & Nachteile, Abwägung/Widersprüche, Empfehlung.
      |- Sei sachlich und begründe die Empfehlung nachvollziehbar.
      |- Antworte auf Deutsch.
      |""".stripMargin

  def apply(): Behavior[AgentProtocol.Command] =
    AgentActor(
      name = "Synthesis-Agent",
      systemPrompt = SystemPrompt,
      useWebSearch = false,
    )

  def prompt(topic: String, facts: String, risks: String): String =
    s"""Thema: $topic
       |
       |<reportOfFactResearcher>
       |$facts
       |</reportOfFactResearcher>
       |
       |<reportOfRiskAnalyst>
       |$risks
       |</reportOfRiskAnalyst>
       |
       |Erstelle nun den finalen, konsolidierten Bericht.""".stripMargin
