package agents

import io.circe.Codec

/** Strukturierte Ausgabe des Risk-Analyst (siehe `AgentRiskAnalyst.scala`) statt eines Freitext-Berichts - analog zu `FactReport.scala` (siehe dort für die Begründung, "Upstream Agent Optimisation").
  *
  * @param description
  *   Das Risiko/der Nachteil/die versteckte Kostenposition, möglichst ein Satz.
  * @param category
  *   Grobe Kategorie, z. B. "Kosten", "Sicherheit", "Betrieb", "Compliance".
  * @param severity
  *   Schweregrad, 1 (gering) bis 5 (kritisch).
  * @param mitigation
  *   Möglicher Gegenmaßnahme-Hinweis, falls vorhanden (`None` sonst).
  */
final case class Risk(
    description: String,
    category: String,
    severity: Int,
    mitigation: Option[String],
) derives Codec

/** Container für alle vom Risk-Analyst identifizierten Risiken plus die separat erfragte TCO-Schätzung (`calculate_tco`, siehe `CalculateTcoTool.scala`) - das, was `AgentRiskAnalyst.analyze`
  * letztlich liefert (siehe dort).
  *
  * Meta-Felder analog zu `FactReport` (siehe dort für die ausführliche Begründung, CCAF 5.3 "Error Propagation in Multi-Agent Systems") - werden von `AgentRiskAnalyst.analyze` gesetzt, nicht vom
  * Modell selbst geliefert.
  *
  * @param tcoEstimate
  *   Menschenlesbare Zusammenfassung des `calculate_tco`-Ergebnisses (als Demo-Berechnung gekennzeichnet), `None` falls das Tool nicht genutzt wurde.
  * @param status
  *   `Success` oder `PartialFailure` - siehe `FactReport.status`/`ReportStatus` (`Agent.scala`).
  * @param failureType
  *   `Transient` oder `Validation` - siehe `FactReport.failureType`/`FailureType` (`Agent.scala`).
  * @param attemptedAction
  *   Was konkret versucht wurde, z. B. `"Risikoanalyse zu '<topic>' (inkl. calculate_tco)"`.
  * @param alternativeApproaches
  *   Vorschläge für den Coordinator bei `PartialFailure`.
  * @param rawOutputSnippet
  *   Unveränderter Rohtext bei `PartialFailure` - siehe `FactReport.rawOutputSnippet`.
  */
final case class RiskReport(
    risks: List[Risk],
    tcoEstimate: Option[String],
    status: ReportStatus,
    failureType: Option[FailureType],
    attemptedAction: Option[String],
    alternativeApproaches: List[String],
    rawOutputSnippet: Option[String],
) derives Codec

/** Wire-Format, das der Risk-Analyst tatsächlich vom Modell zurückbekommt (siehe System-Prompt in `AgentRiskAnalyst.scala`) - bewusst OHNE die Meta-Felder von `RiskReport` (siehe dort).
  */
final private[agents] case class RisksOnly(risks: List[Risk], tcoEstimate: Option[String]) derives Codec
