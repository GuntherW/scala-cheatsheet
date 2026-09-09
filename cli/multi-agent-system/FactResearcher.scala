package agents

/** Worker 1: Fact-Researcher
  *
  * Aufgabe: Reines Sammeln von Argumenten, Fakten und Quellen für eine
  * Technologie/Entscheidung. Bewusst OHNE Bewertung von Risiken - das ist
  * die Aufgabe des Risk-Analyst (Trennung von Zuständigkeiten /
  * "Separation of Concerns" zwischen Agenten).
  *
  * Nutzt das server-seitige Tool `web_search`, damit die Fakten nicht nur
  * aus dem Trainingswissen des Modells stammen, sondern mit aktuellen
  * Quellen belegt werden können (Grounding).
  */
object FactResearcher
    extends Agent(
      name = "Fact-Researcher",
      systemPrompt = """Du bist der Fact-Researcher in einem Multi-Agenten-System.
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
        |""".stripMargin,
      useWebSearch = true,
    ):

  def research(topic: String): String =
    run(s"Sammle Fakten, Argumente und Quellen zu folgendem Thema:\n\n$topic")
