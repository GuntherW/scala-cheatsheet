package agents

/** Worker 3: Aggregator ohne Tools - bekommt beide Worker-Outputs als Kontext und erstellt den finalen Bericht. */
object AgentSynthesis extends Agent(
      name = "Synthesis-Agent",
      systemPrompt = """Du bist der Synthesis-Agent in einem Multi-Agenten-System.
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
                       |""".stripMargin,
    ):

  def synthesize(topic: String, facts: String, risks: String): String =
    run(
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
         |Erstelle nun den finalen, konsolidierten Bericht.""".stripMargin,
      maxTokens = 3000,
    )
