package agents

import kyo.*

/** Gemeinsame Bausteine für alle Agenten - Ersatz für die abstrakte Basisklasse `Agent` im sttp-ai-Original (`Agent.scala`). Da kyo-ai Agenten nicht als Objekte/Instanzen mit Zustand modelliert
  * (jeder `AI.gen`-Aufruf ist bereits für sich ein abgeschlossener, effektvoller Wert), genügen hier freie Funktionen statt einer Klassenhierarchie.
  *
  * Terminologie (wichtig für die CCAF-Zertifizierung, siehe auch sttp-ai-Original für die ausführliche Fassung):
  *   - '''Agent''': Ein LLM-Aufruf (`AI.gen`) mit einem spezifischen System-Prompt (Rolle), der eine klar abgegrenzte Aufgabe löst.
  *   - '''Tool Use / Function Calling''': `Tool.init`-Tools (client-seitig, siehe `CalculateTcoTool.scala`) laufen über `AI.enable`/`AI.gen`'s eingebauten Eval-Loop. Das server-seitige `web_search`
  *     lässt sich damit NICHT ausdrücken (siehe `WebSearchClient.scala`) und läuft daher über einen separaten, handgerollten Pfad.
  *   - '''Orchestrator''' / '''Worker Agent''' / '''Synthesis-/Aggregator-Agent''': siehe `Orchestrator.scala`/`AgentSpec.scala`.
  *   - '''Observe/Budget-Guard''': kyo-ai-Enablements um jeden Model-Turn (Logging, Usage-Tracking, Budget) - siehe `Observability.scala`.
  */
object Agents:

  private def warn(caller: String, message: String): Unit < Sync = Log.warn(s"[LLM:$caller/WARN] $message")

  private def enablements(systemPrompt: String, tools: Seq[Tool[Any]]): Seq[AI.Enablement[Any]] =
    Prompt.init(systemPrompt) +: tools

  /** Zentrale Fallunterscheidung über das Ergebnis eines über `Observability.withObservability` + `LLM.run` + `Abort.run[AIGenException]` discharged Model-Calls - `run` und `runStructured` teilen
    * sich diesen Helper, damit die Fehler-Klassifikation (Success/Budget/Gen-Fehler/Panic) an genau EINER Stelle lebt statt in beiden Methoden dupliziert zu sein. Kein "unsauberes" Ende
    * (Transport-Fehler, Budget überschritten, Eval-Loop ausgeschöpft, ...) wird dabei wie ein regulärer Erfolg behandelt (siehe `AgentRunOutcome`-/`ReportOutcome`-Scaladoc, CCAF 5.3 "Silent
    * Suppression").
    */
  private def discharge[T, R](result: Result[AIGenException, Result[Observability.BudgetExceeded, T]])(
      onSuccess: T => R < Sync,
      onBudget: Observability.BudgetExceeded => R < Sync,
      onFailure: AIGenException => R < Sync,
  ): R < Sync =
    result match
      case Result.Success(Result.Success(value))       => onSuccess(value)
      case Result.Success(Result.Failure(budgetError)) => onBudget(budgetError)
      case Result.Success(Result.Panic(error))         => throw error
      case Result.Failure(genError)                    => onFailure(genError)
      case Result.Panic(error)                         => throw error

  /** Führt einen Model-Call über `AI.gen[String]` aus - Ersatz für `Agent.run` im Original. Discharged `LLM` (via `LLM.run`) und den Budget-Guard-Abort (via `Observability.withObservability`) selbst;
    * der Aufrufer sieht nur noch `Async & Sync` (nie `LLM`/`Abort[BudgetExceeded]`/`Abort[AIGenException]`).
    */
  def run(caller: String, systemPrompt: String, userMessage: String, maxTokens: Int = 2000, tools: Seq[Tool[Any]] = Seq.empty): AgentRunOutcome < (Async & Sync) =
    val generation                                                                                        =
      Observability.withObservability(caller)(AI.enable(enablements(systemPrompt, tools))(AI.gen[String](userMessage)))
    val discharged: Result[AIGenException, Result[Observability.BudgetExceeded, String]] < (Async & Sync) =
      Abort.run[AIGenException](LLM.run(LlmConfig.config.maxTokens(maxTokens))(generation))
    discharged.map { result =>
      discharge(result)(
        onSuccess = text => AgentRunOutcome(text, complete = true, failureType = None),
        onBudget = budgetError =>
          warn(caller, s"Budget überschritten (${budgetError.spentTokens} Tokens) - liefere unvollständiges Ergebnis statt es zu verschleiern.")
            .map(_ => AgentRunOutcome(s"[Budget überschritten nach ${budgetError.spentTokens} Tokens]", complete = false, failureType = Some(FailureType.Transient))),
        onFailure = genError =>
          warn(
            caller,
            s"Kein regulärer Abschluss des Model-Calls (${genError.getClass.getSimpleName}) - liefere unvollständiges Ergebnis statt es zu verschleiern.",
          )
            .map(_ => AgentRunOutcome(AgentRunOutcome.safeMessage(genError), complete = false, failureType = Some(AgentRunOutcome.classify(genError)))),
      )
    }

  /** Wie `run`, aber mit erzwungenem, typsicherem Ergebnis (`AI.gen[T]` statt `AI.gen[String]`, siehe kyo-ai-README "Typed results") - Ersatz für `Agent.runStructured`. Da kyo-ai die
    * Schema-Konformität bereits auf API-Ebene erzwingt (Structured Output) UND client-seitige Tools mit einem typisierten Ergebnis kombinierbar sind (anders als im sttp-ai-Original, siehe
    * `AgentRiskAnalyst.scala`), entfällt das manuelle JSON-Parsing (`JsonExtraction`) hier komplett - ein Decode-Fehler kann bei `AI.gen[T]` praktisch nur noch als `AIDecodeException` (Teil von
    * `AIGenException`) auftreten und wird über `AgentRunOutcome.classify` einheitlich als `Validation` eingeordnet.
    *
    * @param attemptedAction
    *   Freitext-Beschreibung dessen, was versucht wurde - landet nur bei `status = PartialFailure` im `ReportOutcome` (siehe dort).
    */
  def runStructured[T: Schema](
      caller: String,
      systemPrompt: String,
      userMessage: String,
      attemptedAction: String,
      maxTokens: Int = 2000,
      tools: Seq[Tool[Any]] = Seq.empty,
  ): ReportOutcome[T] < (Async & Sync) =
    val generation                                                                                   =
      Observability.withObservability(caller)(AI.enable(enablements(systemPrompt, tools))(AI.gen[T](userMessage)))
    val discharged: Result[AIGenException, Result[Observability.BudgetExceeded, T]] < (Async & Sync) =
      Abort.run[AIGenException](LLM.run(LlmConfig.config.maxTokens(maxTokens))(generation))
    val alternativeApproaches                                                                        = List(
      "Erneut mit engerem Themen-Scope versuchen",
      "runStructured(..., maxTokens = ...) mit höherem Limit erneut aufrufen",
    )
    discharged.map { result =>
      discharge(result)(
        onSuccess = parsed => ReportOutcome(Some(parsed), ReportStatus.Success, None, None, List.empty, None): ReportOutcome[T],
        onBudget = budgetError =>
          warn(caller, s"Budget überschritten (${budgetError.spentTokens} Tokens) - liefere partial_failure statt ein erfundenes Ergebnis vorzutäuschen.").map(_ =>
            ReportOutcome(
              parsed = None,
              status = ReportStatus.PartialFailure,
              failureType = Some(FailureType.Transient),
              attemptedAction = Some(attemptedAction),
              alternativeApproaches = alternativeApproaches,
              rawOutputSnippet = Some(s"[Budget überschritten nach ${budgetError.spentTokens} Tokens]"),
            ),
          ),
        onFailure = genError =>
          warn(
            caller,
            s"Kein regulärer Abschluss des Model-Calls (${genError.getClass.getSimpleName}) - liefere partial_failure statt ein erfundenes Ergebnis vorzutäuschen.",
          ).map(_ =>
            ReportOutcome(
              parsed = None,
              status = ReportStatus.PartialFailure,
              failureType = Some(AgentRunOutcome.classify(genError)),
              attemptedAction = Some(attemptedAction),
              alternativeApproaches = alternativeApproaches,
              rawOutputSnippet = Some(AgentRunOutcome.safeMessage(genError)),
            ),
          ),
      )
    }
