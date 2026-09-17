package agents

import io.circe.syntax.*

/** Worker 1: Fact-Researcher
  *
  * Aufgabe: Reines Sammeln von Argumenten, Fakten und Quellen für eine Technologie/Entscheidung. Bewusst OHNE Bewertung von Risiken - das ist die Aufgabe des Risk-Analyst (Trennung von
  * Zuständigkeiten / "Separation of Concerns" zwischen Agenten).
  *
  * Nutzt das server-seitige Tool `web_search`, damit die Fakten nicht nur aus dem Trainingswissen des Modells stammen, sondern mit aktuellen Quellen belegt werden können (Grounding).
  *
  * '''Upstream Agent Optimisation''' (siehe README): Statt eines ausformulierten Freitext-Berichts liefert dieser Agent strukturierte Daten (`FactReport`, siehe `FactReport.scala`) - einzelne Facts
  * inkl. Quelle/Datum/Relevanz statt Reasoning-Prosa, damit der Synthesis-Agent (Downstream, begrenztes Context-Budget) nicht unnötig Tokens auf nicht verwertbaren Fließtext verschwendet. Da der
  * `web_search`-Pfad (`useWebSearch = true`) kein natives Structured Output unterstützt (siehe `AnthropicClient.buildAgent`), verlangt der System-Prompt reines JSON als Textantwort; das Ergebnis wird
  * anschließend lokal geparst (`JsonExtraction.parseLenient`) statt es serverseitig zu erzwingen.
  */
object AgentFactResearcher extends Agent(
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
                       |- Antworte AUSSCHLIESSLICH mit validem JSON, keine Erklärtexte davor/danach,
                       |  keine Markdown-Codefences, exakt in folgender Form:
                       |  {"facts": [{"claim": "...", "source": "https://... oder null", "date": "z.B. 2025 oder null", "relevance": 1-5}]}
                       |- "claim" ist die eigentliche Tatsachenaussage in einem Satz.
                       |- "relevance" bewertet, wie zentral das Argument für die Entscheidung ist (1=Randnotiz, 5=zentral).
                       |- Antworte auf Deutsch (nur die Textfelder, nicht die JSON-Schlüssel).
                       |""".stripMargin,
      useWebSearch = true,
    ):

  def research(topic: String): FactReport =
    val outcome = runStructured[FactsOnly](
      userMessage = s"Sammle Fakten, Argumente und Quellen zu folgendem Thema:\n\n$topic",
      attemptedAction = s"web_search-Recherche zu '$topic'",
    )
    FactReport(
      status = outcome.status,
      failureType = outcome.failureType,
      attemptedAction = outcome.attemptedAction,
      facts = outcome.parsed.map(_.facts).getOrElse(List.empty),
      alternativeApproaches = outcome.alternativeApproaches,
      rawOutputSnippet = outcome.rawOutputSnippet,
    )

  /** Generische Beschreibung für Registry/Planner (siehe `AgentSpec.scala`). Keine Abhängigkeiten - kann daher parallel zu anderen abhängigkeitsfreien Agenten laufen.
    *
    * `execute` liefert das strukturierte `FactReport` als kompaktes JSON (statt Freitext) an nachgelagerte Agenten weiter (siehe `AgentSynthesis.scala`).
    */
  def spec(topic: String): AgentSpec = AgentSpec(
    id = name,
    description = "Sammelt objektive Fakten, Vorteile und Quellen FÜR die gegebene Technologie/Entscheidung (nutzt web_search). Nennt keine Risiken.",
    execute = _ => research(topic).asJson.noSpaces,
  )
