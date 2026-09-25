package agents

/** Der agentische Kern des Systems: Ein LLM-Aufruf, der - im Gegensatz zum bisherigen, komplett hartcodierten Ablauf - selbst entscheidet, WELCHE der registrierten Agenten (siehe
  * `AgentRegistry`/`AgentSpec`) für das gegebene Thema aufgerufen werden sollen, in welcher Reihenfolge und was parallel laufen kann.
  *
  * Das unterscheidet einen "Workflow" (fixer, vorprogrammierter Kontrollfluss - der bisherige `Orchestrator`) von einem "Agent" (das Modell selbst trifft die Kontrollfluss-Entscheidung). Hier
  * verschieben wir genau diese eine Entscheidung - "welche Worker in welcher Gruppierung" - vom Scala-Code ins Modell, der eigentliche Worker-Aufruf (`AgentSpec.execute`) bleibt normaler,
  * deterministischer Code.
  *
  * Technisch: Nutzt Anthropics natives '''Structured Output''' (`output_config`/`json_schema`, siehe `AnthropicClient.buildStructuredAgent`), NICHT Tool-Use. Der Unterschied: Bei Tool-Use könnte das
  * Modell trotz Tool-Definition mit einem reinen Text-Turn antworten (`stopReason != "tool_use"`); Structured Output erzwingt dagegen direkt auf API-Ebene, dass die GESAMTE Antwort exakt dem
  * JSON-Schema von `ExecutionPlan` entspricht - ein Multi-Turn-Loop wie bei Tool-Use-Agenten (siehe `Agent.scala`) ist daher nicht nötig, ein einzelner Request genügt.
  *
  * Da die Antwort eines LLM nie hundertprozentig verlässlich ist (unbekannte agent-ids, verletzte Abhängigkeiten, vergessene Pflicht-Agenten), wird der rohe Plan vor der Ausführung immer durch
  * `PlanValidator.validate` geschickt (siehe `Orchestrator.runPipeline`).
  */
object AgentPlanner:

  private val model = "vertex/claude-sonnet-5@eu"

  private def systemPrompt(specs: List[AgentSpec]): String =
    val catalogue = specs
      .map { s =>
        val deps = if s.hardDependsOn.isEmpty then "keine" else s.hardDependsOn.mkString(", ")
        val mand = if s.isMandatory then " [PFLICHT - muss immer im Plan enthalten sein]" else ""
        s"- id=\"${s.id}\": ${s.description} (zwingende Abhängigkeiten: $deps)$mand"
      }
      .mkString("\n")

    s"""Du bist der Orchestrator-Agent eines Multi-Agenten-Systems. Deine Aufgabe: Entscheide, welche der
       |folgenden Worker-Agenten für das gegebene Thema aufgerufen werden sollen, in welcher Reihenfolge und
       |welche davon parallel laufen können.
       |
       |Verfügbare Worker-Agenten:
       |$catalogue
       |
       |Regeln:
       |- Ein Agent darf NUR nach allen seinen zwingenden Abhängigkeiten laufen (unterschiedlicher Step).
       |- Agenten OHNE gemeinsame Abhängigkeit sollen im selben Step (parallel) gruppiert werden, um Laufzeit
       |  zu sparen - das ist ausdrücklich erwünscht.
       |- Als PFLICHT markierte Agenten MÜSSEN im Plan enthalten sein.
       |- Nicht als Pflicht markierte Agenten darfst du weglassen, falls sie für das konkrete Thema keinen
       |  Mehrwert liefern würden (z. B. weil eine der beiden Perspektiven für dieses Thema irrelevant ist) -
       |  begründe das kurz in `reasoning`.
       |- `finalAgentId` muss die id des Agenten sein, dessen Ausgabe das Endergebnis der Pipeline darstellt.
       |- Verwende ausschließlich die oben aufgeführten agent-ids, keine erfundenen.
       |""".stripMargin

  /** Erstellt den (noch unvalidierten) Ausführungsplan für `topic` anhand der übergebenen Agenten-Kataloge. Der Aufrufer (`Orchestrator.runPipeline`) MUSS das Ergebnis vor der Ausführung durch
    * `PlanValidator.validate` schicken.
    *
    * Nutzt `AnthropicClient.buildStructuredAgent` (Structured Output über den Interceptor-fähigen sttp-ai Agent-Loop, siehe `AnthropicClient.buildAgent`-Scaladoc) statt eines direkten
    * `chatStructured`-Aufrufs - damit läuft auch die Planungsphase durchs Logging-/Usage-Tracking (`AnthropicClient.usageCollector`), nicht nur die Worker-Agenten.
    */
  def plan(topic: String, specs: List[AgentSpec]): ExecutionPlan =
    val agent = AnthropicClient.buildStructuredAgent[ExecutionPlan](
      caller = "Planner",
      model = model,
      systemPrompt = systemPrompt(specs),
    )
    agent.run(s"Thema: $topic\n\nErstelle den Ausführungsplan.")(AnthropicClient.backend).finalAnswer match
      case Right(executionPlan) => executionPlan
      case Left(failure)        => throw new RuntimeException(s"Planner lieferte keinen validen ExecutionPlan: $failure")
