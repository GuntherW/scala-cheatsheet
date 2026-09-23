package agents

import kyo.*

/** Worker 3: Synthesis-Agent
  *
  * Aufgabe: Liest die strukturierten Ausgaben beider Worker (Fact-Researcher & Risk-Analyst), löst Widersprüche auf und erstellt den finalen, ausgewogenen Freitext-Bericht. Bekommt KEIN eigenes Tool -
  * typisches "Aggregator-Pattern" in Multi-Agenten-Systemen.
  *
  * '''Error Propagation''' (siehe CCAF 5.3): Dieser Agent ist der "Coordinator" im Sinne der Domäne - er bekommt über `status`/`failureType`/`attemptedAction` der beiden Worker-Reports explizit
  * mitgeteilt, falls einer davon nur unvollständig (`partial_failure`) statt gar nicht oder unbemerkt geliefert wurde, und MUSS das im finalen Bericht als Coverage-Annotation transparent machen.
  */
object AgentSynthesis:

  val name = "Synthesis-Agent"

  private val systemPrompt = p"""Du bist der Synthesis-Agent in einem Multi-Agenten-System.
        Du erhältst zwei JSON-Reports (keine Freitext-Berichte):
        1. Fakten vom Fact-Researcher: {"status", "failureType", "attemptedAction",
        "facts": [{"claim", "source", "date", "relevance" (1-5)}], "alternativeApproaches", "rawOutputSnippet"}
        2. Risiken vom Risk-Analyst: {"status", "failureType", "attemptedAction",
        "risks": [{"description", "category", "severity" (1-5), "mitigation"}], "tcoEstimate",
        "alternativeApproaches", "rawOutputSnippet"}
        WICHTIG zu "status":
        - "success" bedeutet: der jeweilige Worker-Agent hat sauber gearbeitet. Eine leere
        "facts"/"risks"-Liste ist dann ein VALIDES Ergebnis (es gibt schlicht nichts
        Nennenswertes) - kein Fehler, keine Erwähnung als Lücke nötig.
        - "partial_failure" bedeutet: der Worker-Agent konnte NICHT zuverlässig liefern
        (siehe "failureType"/"attemptedAction"). Das ist ein ECHTER Ausfall, kein
        valides leeres Ergebnis.
        Deine Aufgabe:
        - Fasse beide JSON-Reports zu einem einzigen, ausgewogenen Freitext-Bericht zusammen.
        - Gewichte Fakten/Risiken mit hoher relevance/severity stärker als solche mit
        niedrigen Werten.
        - Zitiere die "source" eines Facts, falls vorhanden.
        - Löse inhaltliche Widersprüche zwischen den beiden Reports transparent auf
        (z. B. wenn ein Risk eine Aussage aus den Facts relativiert).
        - Falls ein Report "status": "partial_failure" hat: erwähne das EXPLIZIT in einem
        eigenen Abschnitt "Datenabdeckung" (z. B. "Der Abschnitt Risiken ist unvollständig,
        da <attemptedAction> nicht abgeschlossen werden konnte"). Erfinde KEINE Fakten/
        Risiken, um die Lücke zu kaschieren.
        - Gliedere den finalen Bericht in: Zusammenfassung, Fakten & Argumente,
        Risiken & Nachteile, Datenabdeckung (nur falls mind. ein Report partial_failure
        ist), Abwägung/Widersprüche, Empfehlung.
        - Sei sachlich und begründe die Empfehlung nachvollziehbar.
        - Antworte auf Deutsch, in Freitext (kein JSON) - dies ist der finale, für
        Menschen bestimmte Bericht der Pipeline."""

  def synthesize(topic: String, facts: String, risks: String): String < (Async & Sync) =
    val prompt = p"""Thema: $topic
        <factReportJsonOfFactResearcher>
        $facts
        </factReportJsonOfFactResearcher>
        <riskReportJsonOfRiskAnalyst>
        $risks
        </riskReportJsonOfRiskAnalyst>
        Erstelle nun den finalen, konsolidierten Freitext-Bericht."""
    Agents.run(caller = name, systemPrompt = systemPrompt, userMessage = prompt, maxTokens = 6000).map { outcome =>
      if outcome.complete then outcome.text
      else
        s"${outcome.text}\n\n---\n**Hinweis:** Diese Synthese wurde durch ein Ressourcen-/Zeitlimit vorzeitig beendet - der obige Bericht ist möglicherweise unvollständig."
    }

  /** Wird als Ersatz-Kontext genutzt, falls ein Upstream-Agent trotz `hardDependsOn` (siehe `spec` unten) keinen Output geliefert hat - sollte dank `PlanValidator`s transitivem Abschluss über
    * `hardDependsOn` (siehe dort) nicht mehr vorkommen, ist aber als zweite Verteidigungslinie sinnvoll: ein leerer String wäre kein valides JSON gemäß des Prompt-Schemas und würde die im
    * System-Prompt dokumentierte "Datenabdeckung"-Logik unterlaufen (CCAF 5.3 "Silent Suppression") - dieser Platzhalter meldet den Ausfall stattdessen im selben `status`-Format, das der
    * System-Prompt ohnehin erwartet.
    */
  private def missingFactReport(agentId: String): String = Json.encode(
    FactReport(
      status = ReportStatus.PartialFailure,
      failureType = Some(FailureType.Transient),
      attemptedAction = Some(s"Agent '$agentId' wurde laut Ausführungsplan nicht ausgeführt"),
      facts = List.empty,
      alternativeApproaches = List("Ausführungsplan prüfen - dieser Agent hätte laut hardDependsOn zwingend vor Synthesis-Agent laufen müssen."),
      rawOutputSnippet = None,
    ),
  )

  private def missingRiskReport(agentId: String): String = Json.encode(
    RiskReport(
      risks = List.empty,
      tcoEstimate = None,
      status = ReportStatus.PartialFailure,
      failureType = Some(FailureType.Transient),
      attemptedAction = Some(s"Agent '$agentId' wurde laut Ausführungsplan nicht ausgeführt"),
      alternativeApproaches = List("Ausführungsplan prüfen - dieser Agent hätte laut hardDependsOn zwingend vor Synthesis-Agent laufen müssen."),
      rawOutputSnippet = None,
    ),
  )

  /** Generische Beschreibung für Registry/Planner (siehe `AgentSpec.scala`). Hängt zwingend von Fact-Researcher UND Risk-Analyst ab (`hardDependsOn`) und ist als Pflicht-Agent markiert
    * (`isMandatory`), da ohne ihn kein konsolidierter Endbericht entsteht - der `PlanValidator` erzwingt beides (inkl. transitivem Abschluss über `hardDependsOn`), selbst falls der Planungs-Agent
    * (LLM) das anders vorschlagen sollte.
    */
  def spec(topic: String): AgentSpec = AgentSpec(
    id = name,
    description = "Fasst die Ausgaben von Fact-Researcher und Risk-Analyst zu einem ausgewogenen, konsolidierten Endbericht zusammen.",
    hardDependsOn = Set(AgentFactResearcher.name, AgentRiskAnalyst.name),
    isMandatory = true,
    execute = inputs =>
      synthesize(
        topic,
        inputs.getOrElse(AgentFactResearcher.name, missingFactReport(AgentFactResearcher.name)),
        inputs.getOrElse(AgentRiskAnalyst.name, missingRiskReport(AgentRiskAnalyst.name)),
      ),
  )
