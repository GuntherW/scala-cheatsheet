package agents

import org.llm4s.config.DuckDuckGoSearchToolConfig
import org.llm4s.toolapi.builtin.search.DuckDuckGoSearchTool

/** Worker 1: sammelt Fakten/Argumente mit Quellen (Tool: llm4s-eigenes `duckduckgo_search`), bewertet bewusst KEINE Risiken. */
object AgentFactResearcher extends Agent(
      name = "Fact-Researcher",
      systemPrompt = """Du bist der Fact-Researcher in einem Multi-Agenten-System.
                       |
                       |Deine EINZIGE Aufgabe: Sammle objektive Fakten, Argumente und Quellen für die
                       |gegebene Technologie oder Entscheidung.
                       |
                       |Regeln:
                       |- Nenne konkrete Vorteile, Anwendungsfälle und technische Eigenschaften.
                       |- Belege wichtige Aussagen nach Möglichkeit mit Quellen (nutze duckduckgo_search;
                       |  das Tool liefert nur Kurzantworten/Definitionen, ergänze mit eigenem Wissen).
                       |- Bewerte KEINE Risiken, Kosten oder Nachteile - das übernimmt ein anderer Agent.
                       |- Sei präzise und strukturiere die Antwort in Stichpunkten mit Quellenangaben.
                       |- Antworte auf Deutsch.
                       |""".stripMargin,
      tools = Seq(
        DuckDuckGoSearchTool
          .create(DuckDuckGoSearchToolConfig(apiUrl = "https://api.duckduckgo.com"))
          .fold(error => throw new RuntimeException(s"duckduckgo_search konnte nicht gebaut werden: ${error.formatted}"), identity),
      ),
    ):

  def research(topic: String): String = run(s"Sammle Fakten, Argumente und Quellen zu folgendem Thema:\n\n$topic")
