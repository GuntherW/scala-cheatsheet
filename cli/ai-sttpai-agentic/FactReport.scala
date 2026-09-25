package agents

import io.circe.Codec

/** Strukturierte Ausgabe des Fact-Researcher (siehe `AgentFactResearcher.scala`) statt eines Freitext-Berichts.
  *
  * Hintergrund ("Upstream Agent Optimisation", siehe README): Der Synthesis-Agent (Downstream, siehe `AgentSynthesis.scala`) hat ein begrenztes Context-Budget und braucht keine ausformulierten
  * Sätze/Reasoning des Fact-Researcher, sondern nur die eigentlichen Fakten inkl. Metadaten (Quelle, Datum, Relevanz) - strukturierte Daten lassen sich zudem gezielter gewichten/zitieren als
  * Freitext.
  *
  * @param claim
  *   Die eigentliche Tatsachenaussage bzw. das Argument, möglichst ein Satz.
  * @param source
  *   Quelle/URL, falls durch `web_search` belegt (`None`, falls keine externe Quelle zitiert wurde).
  * @param date
  *   Datum der Quelle/Information, falls bekannt (z. B. "2025" oder "2025-03") - `None` falls unbekannt.
  * @param relevance
  *   Relevanz für die Entscheidung, 1 (Randnotiz) bis 5 (zentrales Argument).
  */
final case class Fact(
    claim: String,
    source: Option[String],
    date: Option[String],
    relevance: Int,
) derives Codec

/** Container für alle vom Fact-Researcher gesammelten Fakten - das, was `AgentFactResearcher.research` letztlich liefert (siehe dort).
  *
  * Die Meta-Felder (`status`/`failureType`/`attemptedAction`/`alternativeApproaches`/`rawOutputSnippet`) werden NICHT vom Modell geliefert (das antwortet nur mit `{"facts": [...]}`), sondern von
  * `AgentFactResearcher.research` selbst gesetzt - strukturierte Fehlerweitergabe statt "Silent Suppression" (siehe CCAF 5.3 "Error Propagation in Multi-Agent Systems"): Der Synthesis-Agent
  * (Downstream, siehe `AgentSynthesis.scala`) soll einen fehlgeschlagenen/unvollständigen Recherche-Lauf von einem legitim leeren Ergebnis (`facts = List.empty` bei `status = "success"`, z. B. weil
  * das Thema tatsächlich keine belegbaren Fakten hergibt) unterscheiden können, statt beides gleich zu behandeln oder gar zu verschleiern.
  *
  * @param status
  *   `Success` (Model-Call sauber abgeschlossen UND Antwort war valides JSON - `facts` kann dabei durchaus leer sein, das ist dann ein valider leerer Treffer) oder `PartialFailure` - siehe
  *   `ReportStatus` (`Agent.scala`).
  * @param failureType
  *   `None` bei `status = Success`, sonst `Transient`/`Validation` - siehe `FailureType` (`Agent.scala`).
  * @param attemptedAction
  *   Was konkret versucht wurde (Thema/Tool), z. B. `"web_search-Recherche zu '<topic>'"` - fehlt bei `status = Success`.
  * @param facts
  *   Die eigentlichen Fakten ("partialResults" im Sinne von CCAF 5.3) - bei `PartialFailure` i. d. R. leer, da nichts Verwertbares vorliegt.
  * @param alternativeApproaches
  *   Vorschläge für den Coordinator (Synthesis-Agent/Mensch), wie mit dem Fehlschlag umgegangen werden könnte - leer bei `status = Success`.
  * @param rawOutputSnippet
  *   Der unveränderte, ggf. unvollständige oder nicht JSON-konforme Rohtext des Model-Calls bei `PartialFailure` - wird bewusst NICHT verworfen (sondern separat sichtbar gemacht), aber auch NICHT als
  *   vermeintlich normaler `Fact` getarnt.
  */
final case class FactReport(
    status: ReportStatus,
    failureType: Option[FailureType],
    attemptedAction: Option[String],
    facts: List[Fact],
    alternativeApproaches: List[String],
    rawOutputSnippet: Option[String],
) derives Codec

/** Wire-Format, das der Fact-Researcher tatsächlich vom Modell zurückbekommt (siehe System-Prompt in `AgentFactResearcher.scala`) - bewusst OHNE die Meta-Felder von `FactReport`, die ausschließlich
  * von unserem Code gesetzt werden (siehe dort).
  */
final private[agents] case class FactsOnly(facts: List[Fact]) derives Codec
