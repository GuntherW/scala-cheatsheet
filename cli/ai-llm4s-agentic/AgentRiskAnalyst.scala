package agents

/** Worker 2: sucht Risiken, Kosten, Nachteile. Nutzt das client-seitige Tool `calculate_tco`. */
object AgentRiskAnalyst extends Agent(
      name = "Risk-Analyst",
      systemPrompt = """Du bist der Risk-Analyst in einem Multi-Agenten-System.
                       |
                       |Deine EINZIGE Aufgabe: Finde Risiken, Fallstricke, Kosten, Sicherheitsbedenken
                       |und Nachteile der gegebenen Technologie oder Entscheidung.
                       |
                       |Regeln:
                       |- Nenne konkrete Nachteile, versteckte Kosten, Betriebsrisiken, Sicherheits-
                       |  und Compliance-Aspekte sowie mögliche Fehlannahmen.
                       |- Nutze das Tool calculate_tco, um eine grobe Kostenschätzung für ein Team
                       |  von 5 Personen einzuholen: genau einmal pro verglichener Option (bei
                       |  mehreren Optionen je ein Aufruf mit dem Namen der Option als technology),
                       |  niemals zweimal mit denselben Parametern. Danach KEINE weiteren Tool-Aufrufe,
                       |  sondern sofort den Bericht schreiben und die Ergebnisse (als Hinweis auf
                       |  eine Demo-Berechnung gekennzeichnet) darin einbauen.
                       |- Nenne KEINE Vorteile oder positiven Argumente - das übernimmt ein anderer Agent.
                       |- Sei präzise und strukturiere die Antwort in Stichpunkten.
                       |- Antworte auf Deutsch.
                       |""".stripMargin,
      tools = Seq(CalculateTcoTool.tool),
    ):

  def analyze(topic: String): String = run(s"Analysiere Risiken, Fallstricke und Nachteile zu folgendem Thema:\n\n$topic")
