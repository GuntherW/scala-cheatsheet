package agents

/** Worker 3: Synthesis-Agent
  *
  * Aufgabe: Liest die strukturierten Ausgaben beider Worker (Fact-Researcher & Risk-Analyst, siehe `FactReport.scala`/`RiskReport.scala`), löst Widersprüche auf und erstellt den finalen, ausgewogenen
  * Freitext-Bericht.
  *
  * Dieser Agent bekommt KEIN eigenes Tool (kein web_search), da er nicht selbst recherchiert, sondern ausschließlich mit dem bereits gesammelten Kontext (den Outputs der Worker) arbeitet. Das ist ein
  * typisches "Aggregator-Pattern" in Multi-Agenten-Systemen.
  *
  * '''Upstream Agent Optimisation''' (siehe README): Anders als früher bekommt dieser Agent keine ausformulierten Freitext-Berichte der Worker mehr, sondern deren strukturiertes JSON (`FactReport`/
  * `RiskReport`) - er ist damit der klassische Downstream-Agent mit begrenztem Context-Budget, der auf Fakten/Metadaten (Quelle, Datum, Relevanz, Schweregrad) statt auf Reasoning-Prosa der Upstream-
  * Agenten angewiesen ist. Die Ausgabe DIESES Agenten selbst bleibt bewusst Freitext (Markdown-Bericht), da er das Ende der Pipeline ist und für Menschen lesbar sein soll.
  *
  * '''Error Propagation''' (siehe CCAF 5.3): Dieser Agent ist der "Coordinator" im Sinne der Domäne - er bekommt über `status`/`failureType`/`attemptedAction` der beiden Worker-Reports explizit
  * mitgeteilt, falls einer davon nur unvollständig (`partial_failure`) statt gar nicht (Workflow Termination) oder unbemerkt (Silent Suppression) geliefert wurde, und MUSS das im finalen Bericht als
  * Coverage-Annotation transparent machen statt es zu verschweigen oder Lücken kommentarlos zu überspielen.
  */
object AgentSynthesis extends Agent(
      name = "Synthesis-Agent",
      systemPrompt = """Du bist der Synthesis-Agent in einem Multi-Agenten-System.
                       |
                       |Du erhältst zwei JSON-Reports (keine Freitext-Berichte):
                       |1. Fakten vom Fact-Researcher: {"status", "failureType", "attemptedAction",
                       |   "facts": [{"claim", "source", "date", "relevance" (1-5)}], "alternativeApproaches", "rawOutputSnippet"}
                       |2. Risiken vom Risk-Analyst: {"status", "failureType", "attemptedAction",
                       |   "risks": [{"description", "category", "severity" (1-5), "mitigation"}], "tcoEstimate",
                       |   "alternativeApproaches", "rawOutputSnippet"}
                       |
                       |WICHTIG zu "status":
                       |- "success" bedeutet: der jeweilige Worker-Agent hat sauber gearbeitet. Eine leere
                       |  "facts"/"risks"-Liste ist dann ein VALIDES Ergebnis (es gibt schlicht nichts
                       |  Nennenswertes) - kein Fehler, keine Erwähnung als Lücke nötig.
                       |- "partial_failure" bedeutet: der Worker-Agent konnte NICHT zuverlässig liefern
                       |  (siehe "failureType"/"attemptedAction"). Das ist ein ECHTER Ausfall, kein
                       |  valides leeres Ergebnis.
                       |
                       |Deine Aufgabe:
                       |- Fasse beide JSON-Reports zu einem einzigen, ausgewogenen Freitext-Bericht zusammen.
                       |- Gewichte Fakten/Risiken mit hoher relevance/severity stärker als solche mit
                       |  niedrigen Werten.
                       |- Zitiere die "source" eines Facts, falls vorhanden.
                       |- Löse inhaltliche Widersprüche zwischen den beiden Reports transparent auf
                       |  (z. B. wenn ein Risk eine Aussage aus den Facts relativiert).
                       |- Falls ein Report "status": "partial_failure" hat: erwähne das EXPLIZIT in einem
                       |  eigenen Abschnitt "Datenabdeckung" (z. B. "Der Abschnitt Risiken ist unvollständig,
                       |  da <attemptedAction> nicht abgeschlossen werden konnte"). Erfinde KEINE Fakten/
                       |  Risiken, um die Lücke zu kaschieren.
                       |- Gliedere den finalen Bericht in: Zusammenfassung, Fakten & Argumente,
                       |  Risiken & Nachteile, Datenabdeckung (nur falls mind. ein Report partial_failure
                       |  ist), Abwägung/Widersprüche, Empfehlung.
                       |- Sei sachlich und begründe die Empfehlung nachvollziehbar.
                       |- Antworte auf Deutsch, in Freitext (kein JSON) - dies ist der finale, für
                       |  Menschen bestimmte Bericht der Pipeline.
                       |""".stripMargin,
      useWebSearch = false,
    ):

  def synthesize(topic: String, facts: String, risks: String): String =
    val prompt  = s"""Thema: $topic
                     |
                     |<factReportJsonOfFactResearcher>
                     |$facts
                     |</factReportJsonOfFactResearcher>
                     |
                     |<riskReportJsonOfRiskAnalyst>
                     |$risks
                     |</riskReportJsonOfRiskAnalyst>
                     |
                     |Erstelle nun den finalen, konsolidierten Freitext-Bericht.""".stripMargin
    val outcome = run(prompt, maxTokens = 3000)
    if outcome.complete then outcome.text
    else
      warn(s"Synthese selbst nicht sauber abgeschlossen (failureType=${outcome.failureType.map(_.wireValue).getOrElse("?")}) - Hinweis wird an den Bericht angehängt.")
      outcome.text + "\n\n---\n**Hinweis:** Diese Synthese wurde durch ein Ressourcen-/Zeitlimit vorzeitig beendet - der obige Bericht ist möglicherweise unvollständig."

  /** Generische Beschreibung für Registry/Planner (siehe `AgentSpec.scala`). Hängt zwingend von Fact-Researcher UND Risk-Analyst ab (`hardDependsOn`) und ist als Pflicht-Agent markiert
    * (`isMandatory`), da ohne ihn kein konsolidierter Endbericht entsteht - der `PlanValidator` erzwingt beides, selbst falls der Planungs-Agent (LLM) das anders vorschlagen sollte.
    */
  def spec(topic: String): AgentSpec = AgentSpec(
    id = name,
    description = "Fasst die Ausgaben von Fact-Researcher und Risk-Analyst zu einem ausgewogenen, konsolidierten Endbericht zusammen.",
    hardDependsOn = Set(AgentFactResearcher.name, AgentRiskAnalyst.name),
    isMandatory = true,
    execute = inputs => synthesize(topic, inputs.getOrElse(AgentFactResearcher.name, ""), inputs.getOrElse(AgentRiskAnalyst.name, "")),
  )
