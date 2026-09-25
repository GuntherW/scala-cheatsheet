package agents

import kyo.*

/** Worker 1: Fact-Researcher
  *
  * Aufgabe: Reines Sammeln von Argumenten, Fakten und Quellen für eine Technologie/Entscheidung. Bewusst OHNE Bewertung von Risiken - das ist die Aufgabe des Risk-Analyst (Separation of Concerns
  * zwischen Agenten).
  *
  * Nutzt das server-seitige Anthropic-Tool `web_search`, damit die Fakten nicht nur aus dem Trainingswissen des Modells stammen (Grounding) - siehe `WebSearchClient.scala` für die Begründung, warum
  * das über einen handgerollten Einzel-Request-Pfad statt über `AI.gen`/`Tool.init` laufen muss.
  *
  * '''Upstream Agent Optimisation''' (siehe README): Statt eines ausformulierten Freitext-Berichts liefert dieser Agent strukturierte Daten (`FactReport`). Da der `web_search`-Pfad außerhalb von
  * `AI.gen` läuft und daher KEIN natives Structured Output erzwingen kann, verlangt der System-Prompt reines JSON als Textantwort; das Ergebnis wird anschließend lokal decodiert
  * (`JsonExtraction.parseLenient`) statt es serverseitig zu erzwingen - exakt wie im sttp-ai-Original.
  */
object AgentFactResearcher:

  val name = "Fact-Researcher"

  private val systemPrompt = p"""Du bist der Fact-Researcher in einem Multi-Agenten-System.
        Deine EINZIGE Aufgabe: Sammle objektive Fakten, Argumente und Quellen für die
        gegebene Technologie oder Entscheidung.
        Regeln:
        - Nenne konkrete Vorteile, Anwendungsfälle und technische Eigenschaften.
        - Belege wichtige Aussagen nach Möglichkeit mit Quellen (nutze web_search).
        - Bewerte KEINE Risiken, Kosten oder Nachteile - das übernimmt ein anderer Agent.
        - Antworte AUSSCHLIESSLICH mit validem JSON, keine Erklärtexte davor/danach,
        keine Markdown-Codefences, exakt in folgender Form:
        {"facts": [{"claim": "...", "source": "https://... oder null", "date": "z.B. 2025 oder null", "relevance": 1-5}]}
        - "claim" ist die eigentliche Tatsachenaussage in einem Satz.
        - "relevance" bewertet, wie zentral das Argument für die Entscheidung ist (1=Randnotiz, 5=zentral).
        - Antworte auf Deutsch (nur die Textfelder, nicht die JSON-Schlüssel)."""

  def research(topic: String): FactReport < (Async & Sync) =
    val attemptedAction                            = s"web_search-Recherche zu '$topic'"
    val userMessage                                = s"Sammle Fakten, Argumente und Quellen zu folgendem Thema:\n\n$topic"
    val raw: Result[HttpException, String] < Async =
      Abort.run[HttpException](
        WebSearchClient.research(
          baseUrl = LlmConfig.baseUrl,
          apiKey = LlmConfig.apiKey,
          model = LlmConfig.model,
          systemPrompt = systemPrompt,
          userMessage = userMessage,
          maxTokens = 4000,
        ),
      )
    raw.map(toFactReport(attemptedAction, _))

  private def toFactReport(attemptedAction: String, result: Result[HttpException, String]): FactReport < Sync =
    result match
      case Result.Success(text)      =>
        JsonExtraction.parseLenient[FactsOnly](text) match
          case Right(parsed) => FactReport(ReportStatus.Success, None, None, parsed.facts, List.empty, None)
          case Left(error)   =>
            Log.warn(s"[LLM:$name/WARN] Konnte web_search-Antwort nicht decodieren ($error) - liefere partial_failure statt ein erfundenes Ergebnis vorzutäuschen.").map(_ =>
              FactReport(
                status = ReportStatus.PartialFailure,
                failureType = Some(FailureType.Validation),
                attemptedAction = Some(attemptedAction),
                facts = List.empty,
                alternativeApproaches = List("Erneut anfragen - Modell hat sich nicht ans verlangte JSON-Schema gehalten."),
                rawOutputSnippet = Some(text),
              ),
            )
      case Result.Failure(httpError) =>
        Log.warn(s"[LLM:$name/WARN] Kein regulärer Abschluss des web_search-Calls (${httpError.getClass.getSimpleName}) - liefere unvollständiges Ergebnis statt es zu verschleiern.").map(_ =>
          FactReport(
            status = ReportStatus.PartialFailure,
            failureType = Some(FailureType.Transient),
            attemptedAction = Some(attemptedAction),
            facts = List.empty,
            alternativeApproaches = List("Erneut mit engerem Themen-Scope versuchen", "WebSearchClient.research(..., maxTokens = ...) mit höherem Limit erneut aufrufen"),
            rawOutputSnippet = Some(AgentRunOutcome.safeMessage(httpError)),
          ),
        )
      case Result.Panic(error)       => throw error

  /** Generische Beschreibung für Registry/Planner (siehe `AgentSpec.scala`). Keine Abhängigkeiten - kann daher parallel zu anderen abhängigkeitsfreien Agenten laufen. `execute` liefert das
    * strukturierte `FactReport` als kompaktes JSON (statt Freitext) an nachgelagerte Agenten weiter (siehe `AgentSynthesis.scala`).
    */
  def spec(topic: String): AgentSpec = AgentSpec(
    id = name,
    description = "Sammelt objektive Fakten, Vorteile und Quellen FÜR die gegebene Technologie/Entscheidung (nutzt web_search). Nennt keine Risiken.",
    execute = _ => research(topic).map(Json.encode(_)),
  )
