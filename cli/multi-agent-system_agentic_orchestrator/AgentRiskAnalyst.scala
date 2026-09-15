package agents

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
