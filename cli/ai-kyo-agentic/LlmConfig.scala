package agents

import kyo.*

/** Zentrale LLM-Konfiguration: liefert das `AI.Config` (Requesty-Router, Modell, Key aus `.env`), das alle Agenten (siehe `Agents.scala`) über `LLM.run` nutzen, sowie die rohen Werte (`baseUrl`,
  * `apiKey`, `model`), die `WebSearchClient` für den handgerollten `web_search`-Pfad braucht (siehe dort).
  *
  * Dieses Projekt spricht (wie das sttp-ai-Original) einen requesty.ai-Router statt der offiziellen Anthropic-Basis-URL an - `AI.Config.Anthropic.default.apiUrl(...)` unterstützt das direkt (Standard
  * wäre `https://api.anthropic.com`).
  */
object LlmConfig:

  /** Basis-URL für `WebSearchClient` (siehe dort), das selbst `$baseUrl/v1/messages` zusammensetzt - OHNE das abschließende `/v1`. */
  val baseUrl: String = "https://router.eu.requesty.ai"

  val model: String = "vertex/claude-sonnet-5@eu"

  val apiKey: String = Env.get("ANTHROPIC_AUTH_TOKEN")
    .orElse(Env.get("ANTHROPIC_API_KEY"))
    .getOrElse(throw new RuntimeException("Weder ANTHROPIC_AUTH_TOKEN noch ANTHROPIC_API_KEY gesetzt."))

  // kyo-ais AnthropicCompletion-Backend haengt selbst nur "/messages" (ohne "/v1") an `apiUrl` an - anders als
  // WebSearchClient (siehe dort), das den vollen Pfad selbst zusammensetzt. Damit beide denselben Router treffen,
  // MUSS `apiUrl` hier bereits das "/v1"-Segment enthalten.
  //
  // Reasoning bleibt bewusst AN (kyo-ai-Default): Ein Test mit `disableReasoning` gegen genau diesen Router zeigte,
  // dass das Modell bei einem groesseren Input-Kontext (z. B. die beiden JSON-Reports im Synthesis-Prompt, siehe
  // `AgentSynthesis.scala`) wiederholt eine nicht schema-konforme Antwort ueber den erzwungenen "Result-Tool"-
  // Mechanismus lieferte (`AIEvalExhaustedException` nach 5 Iterationen + Repair-Turn) - mit aktivem Reasoning
  // gelang derselbe Aufruf sofort. Ein Stolperstein dieses spezifischen Routers/Modells, analog zu den im
  // sttp-ai-Original dokumentierten Router-Eigenheiten (siehe README).
  val config: AI.Config = AI.Config.Anthropic.default
    .apiUrl(s"$baseUrl/v1")
    .apiKey(apiKey)
    .modelName(model)
    .timeout(90.seconds)
    .retrySchedule(Schedule.repeat(2))

/** Lädt Umgebungsvariablen aus der `.env`-Datei im aktuellen Arbeitsverzeichnis (`os.pwd`, also i. d. R. diesem Projektordner, wenn `scala-cli run .` von hier aus gestartet wird), analog zum
  * sttp-ai-Original (`AnthropicClient.Env`) und dessen Python-Pendant (`python-dotenv`).
  *
  * Eigene, bewusst simple Implementierung auf Basis von `os-lib`. Unterstütztes Format: `SCHLÜSSEL=WERT` pro Zeile, `#`-Kommentare und Leerzeilen werden ignoriert, ein- oder doppelte
  * Anführungszeichen um den Wert werden entfernt. Fällt automatisch auf echte Umgebungsvariablen zurück, falls die `.env`-Datei fehlt oder der Schlüssel dort nicht gesetzt ist.
  */
object Env:

  private val dotenvPath = os.pwd / ".env"

  private val dotenvVars: Map[String, String] =
    if os.exists(dotenvPath) then
      os.read
        .lines(dotenvPath)
        .iterator
        .map(_.trim)
        .filter(line => line.nonEmpty && !line.startsWith("#"))
        .flatMap(_.split("=", 2) match
          case Array(key, value) => Some(key.trim -> unquote(value.trim))
          case _                 => None,
        )
        .toMap
    else Map.empty

  private def unquote(value: String): String =
    val isQuoted = value.length >= 2 && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))
    if isQuoted then value.substring(1, value.length - 1) else value

  def get(key: String): Option[String] = dotenvVars.get(key).orElse(sys.env.get(key))
