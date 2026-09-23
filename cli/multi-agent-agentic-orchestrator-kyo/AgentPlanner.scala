package agents

import kyo.*

/** Der agentische Kern des Systems: Ein LLM-Aufruf, der - im Gegensatz zu einem komplett hartcodierten Ablauf - selbst entscheidet, WELCHE der registrierten Agenten (siehe
  * `AgentRegistry`/`AgentSpec`) für das gegebene Thema aufgerufen werden sollen, in welcher Reihenfolge und was parallel laufen kann.
  *
  * Technisch: Nutzt `AI.gen[ExecutionPlan]` (Structured Output, siehe `Agents.runStructured`) - kein manuelles `deriveResponseSchema` wie im sttp-ai-Original nötig, `ExecutionPlan derives Schema`
  * genügt.
  *
  * Da die Antwort eines LLM nie hundertprozentig verlässlich ist (unbekannte agent-ids, verletzte Abhängigkeiten, vergessene Pflicht-Agenten), wird der rohe Plan vor der Ausführung immer durch
  * `PlanValidator.validate` geschickt (siehe `Orchestrator.runPipeline`).
  */
object AgentPlanner:

  private def systemPrompt(specs: List[AgentSpec]): String =
    val catalogue = specs
      .map { s =>
        val deps = if s.hardDependsOn.isEmpty then "keine" else s.hardDependsOn.mkString(", ")
        val mand = if s.isMandatory then " [PFLICHT - muss immer im Plan enthalten sein]" else ""
        s"- id=\"${s.id}\": ${s.description} (zwingende Abhängigkeiten: $deps)$mand"
      }
      .mkString("\n")

    p"""Du bist der Orchestrator-Agent eines Multi-Agenten-Systems. Deine Aufgabe: Entscheide, welche der
        folgenden Worker-Agenten für das gegebene Thema aufgerufen werden sollen, in welcher Reihenfolge und
        welche davon parallel laufen können.
        Verfügbare Worker-Agenten:
        $catalogue
        Regeln:
        - Ein Agent darf NUR nach allen seinen zwingenden Abhängigkeiten laufen (unterschiedlicher Step).
        - Agenten OHNE gemeinsame Abhängigkeit sollen im selben Step (parallel) gruppiert werden, um Laufzeit
        zu sparen - das ist ausdrücklich erwünscht.
        - Als PFLICHT markierte Agenten MÜSSEN im Plan enthalten sein.
        - Nicht als Pflicht markierte Agenten darfst du weglassen, falls sie für das konkrete Thema keinen
        Mehrwert liefern würden (z. B. weil eine der beiden Perspektiven für dieses Thema irrelevant ist) -
        begründe das kurz in `reasoning`.
        - `finalAgentId` muss die id des Agenten sein, dessen Ausgabe das Endergebnis der Pipeline darstellt.
        - Verwende ausschließlich die oben aufgeführten agent-ids, keine erfundenen."""

  /** Erstellt den (noch unvalidierten) Ausführungsplan für `topic` anhand der übergebenen Agenten-Kataloge. Der Aufrufer (`Orchestrator.runPipeline`) MUSS das Ergebnis vor der Ausführung durch
    * `PlanValidator.validate` schicken.
    *
    * Nutzt `Agents.runStructured` (statt eines direkten `AI.gen`-Aufrufs) - damit läuft auch die Planungsphase durchs Logging-/Usage-Tracking (`Observability.usageCollector`), nicht nur die
    * Worker-Agenten.
    */
  def plan(topic: String, specs: List[AgentSpec]): ExecutionPlan < (Async & Sync) =
    Agents
      .runStructured[ExecutionPlan](
        caller = "Planner",
        systemPrompt = systemPrompt(specs),
        userMessage = s"Thema: $topic\n\nErstelle den Ausführungsplan.",
        attemptedAction = s"Planung für Thema '$topic'",
      )
      .map { outcome =>
        outcome.parsed.getOrElse(
          throw new RuntimeException(s"Planner lieferte keinen validen ExecutionPlan (status=${outcome.status}): ${outcome.rawOutputSnippet.getOrElse("")}"),
        )
      }
