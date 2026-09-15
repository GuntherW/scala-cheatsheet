package agents

/** Worker 3: Synthesis-Agent
  *
  * Aufgabe: Liest die Ausgaben beider Worker (Fact-Researcher & Risk-Analyst), löst Widersprüche auf und erstellt den finalen, ausgewogenen Bericht.
  *
  * Dieser Agent bekommt KEIN eigenes Tool (kein web_search), da er nicht selbst recherchiert, sondern ausschließlich mit dem bereits gesammelten Kontext (den Outputs der Worker) arbeitet. Das ist ein
  * typisches "Aggregator-Pattern" in Multi-Agenten-Systemen.
  */
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
      useWebSearch = false,
    ):

  def synthesize(topic: String, facts: String, risks: String): String =
    val prompt = s"""Thema: $topic
                    |
                    |<reportOfFactResearcher>
                    |$facts
                    |</reportOfFactResearcher>
                    |
                    |<reportOfRiskAnalyst>
                    |$risks
                    |</reportOfRiskAnalyst>
                    |
                    |Erstelle nun den finalen, konsolidierten Bericht.""".stripMargin
    run(prompt, maxTokens = 3000)

  val Id = "synthesis"

  /** Generische Beschreibung für Registry/Planner (siehe `AgentSpec.scala`). Hängt zwingend von Fact-Researcher UND Risk-Analyst ab (`hardDependsOn`) und ist als Pflicht-Agent markiert
    * (`isMandatory`), da ohne ihn kein konsolidierter Endbericht entsteht - der `PlanValidator` erzwingt beides, selbst falls der Planungs-Agent (LLM) das anders vorschlagen sollte.
    */
  def spec(topic: String): AgentSpec = AgentSpec(
    id = Id,
    description = "Fasst die Ausgaben von Fact-Researcher und Risk-Analyst zu einem ausgewogenen, konsolidierten Endbericht zusammen.",
    hardDependsOn = Set(AgentFactResearcher.Id, AgentRiskAnalyst.Id),
    isMandatory = true,
    execute = inputs => synthesize(topic, inputs.getOrElse(AgentFactResearcher.Id, ""), inputs.getOrElse(AgentRiskAnalyst.Id, "")),
  )
