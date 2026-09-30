package agent

/** Einziger Agent des Systems (kein Multi-Agenten-System, kein Orchestrator).
  *
  * Terminologie (Fortsetzung von `ai-sttpai-manual/Agent.scala`):
  *   - '''Tool Search / Progressive Tool Disclosure''': Statt dem Modell von Anfang an ALLE Tool-Definitionen mitzugeben, sieht es zunächst nur ein Meta-Tool (`search_tools`). Erst wenn das Modell
  *     darüber ein passendes Tool "findet", wird dessen echtes Schema für die folgenden Turns freigeschaltet. Vorteil in echten Systemen mit vielen (ggf. hunderten) Tools: kein unnötiger
  *     Context-Verbrauch durch Schemas, die für die aktuelle Aufgabe irrelevant sind.
  *   - '''Tool-Registry''': Die zentrale Liste aller tatsächlich verfügbaren (aber anfangs verborgenen) Tools, hier `ToolRegistry`.
  */
object Agent:

  val Model = "vertex/claude-sonnet-5@eu"

  private val systemPrompt =
    """Du bist ein hilfreicher Assistent mit Zugriff auf Werkzeuge (Tools).
      |
      |WICHTIG: Du siehst die verfügbaren Fach-Werkzeuge NICHT direkt. Rufe IMMER zuerst
      |das Werkzeug 'search_tools' mit einer kurzen Beschreibung deines Vorhabens auf
      |(z. B. "aktuelle Uhrzeit ermitteln" oder "Würfel werfen"). Erst danach wird dir
      |das passende Werkzeug zur direkten Nutzung angeboten.
      |
      |Regeln:
      |- Nutze für jede Teilaufgabe, die ein Werkzeug erfordert (Zeit, Rechnen, Würfeln),
      |  zuerst search_tools, dann das gefundene Werkzeug.
      |- Beantworte reine Wissens-/Meinungsfragen ohne Tool direkt.
      |- Fasse am Ende alle Ergebnisse in einer klaren, kurzen Antwort zusammen.
      |- Antworte auf Deutsch.
      |""".stripMargin

  def run(userMessage: String, maxTokens: Int = 2000): String =
    AnthropicClient.chat(model = Model, systemPrompt = systemPrompt, userMessage = userMessage, maxTokens = maxTokens)
