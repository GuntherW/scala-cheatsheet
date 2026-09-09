package agents

import org.apache.pekko.actor.typed.Behavior

/** Worker 1: Fact-Researcher-Aktor.
  *
  * Fachlich identisch zu `FactResearcher` in `research_scala`: Sammelt
  * ausschliesslich Fakten, Argumente und Quellen (server-seitiges Tool
  * `web_search`), bewertet KEINE Risiken (Separation of Concerns
  * zwischen Agenten/Aktoren).
  *
  * Als Aktor lebt dieser Worker dauerhaft als Kind-Aktor des
  * `Orchestrator` und verarbeitet nacheinander `AgentProtocol.Run`-
  * Nachrichten fuer beliebig viele Pipeline-Durchlaeufe.
  */
object FactResearcherActor:

  private val SystemPrompt =
    """Du bist der Fact-Researcher in einem Multi-Agenten-System.
      |
      |Deine EINZIGE Aufgabe: Sammle objektive Fakten, Argumente und Quellen für die
      |gegebene Technologie oder Entscheidung.
      |
      |Regeln:
      |- Nenne konkrete Vorteile, Anwendungsfälle und technische Eigenschaften.
      |- Belege wichtige Aussagen nach Möglichkeit mit Quellen (nutze web_search).
      |- Bewerte KEINE Risiken, Kosten oder Nachteile - das übernimmt ein anderer Agent.
      |- Sei präzise und strukturiere die Antwort in Stichpunkten mit Quellenangaben.
      |- Antworte auf Deutsch.
      |""".stripMargin

  def apply(): Behavior[AgentProtocol.Command] =
    AgentActor(
      name = "Fact-Researcher",
      systemPrompt = SystemPrompt,
      useWebSearch = true,
    )

  def prompt(topic: String): String =
    s"Sammle Fakten, Argumente und Quellen zu folgendem Thema:\n\n$topic"
