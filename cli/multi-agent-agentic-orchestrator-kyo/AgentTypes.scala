package agents

import kyo.*

/** Grobe Fehlerkategorie nach dem CCAF-5.3-Schema ("Error Propagation in Multi-Agent Systems"): `Transient` (Budget/Token-Limit/Transport-Fehler - ein Retry mit angepassten Parametern könnte helfen)
  * oder `Validation` (der Model-Call selbst lief sauber durch, aber die Antwort entsprach nicht dem verlangten JSON-Schema). Wird sowohl von `Agents.run` (technischer Abbruch, siehe
  * `AgentRunOutcome`) als auch von `Agents.runStructured` (Decode-Fehler, siehe `ReportOutcome`) genutzt - aus Coordinator-Sicht (`AgentSynthesis`) sind beides gleichwertige "partial_failure"-Gründe.
  *
  * Serialisiert bewusst als schlichter lowercase-String (`"transient"`/`"validation"`) statt als verschachteltes `{"FailureType": "Transient"}` - das flache JSON-Schema wird 1:1 im System-Prompt von
  * `AgentSynthesis` dokumentiert und muss dazu passen.
  */
enum FailureType derives CanEqual:
  case Transient, Validation

object FailureType:
  given Schema[FailureType] = Schema.derived[FailureType].variantNames(
    "Transient"  -> "transient",
    "Validation" -> "validation",
  )

/** Status eines strukturierten Worker-Reports (`FactReport`/`RiskReport`, siehe `ReportOutcome`) - `"success"` (auch bei leeren `facts`/`risks`, siehe dort) oder `"partial_failure"`. Gleiches
  * Wire-Format-Prinzip wie `FailureType` (siehe dort für die Begründung).
  */
enum ReportStatus derives CanEqual:
  case Success, PartialFailure

object ReportStatus:
  given Schema[ReportStatus] = Schema.derived[ReportStatus].variantNames(
    "Success"        -> "success",
    "PartialFailure" -> "partial_failure",
  )

/** Ergebnis eines rohen Model-Calls (`Agents.run`) - im Gegensatz zu einem reinen `String`-Rückgabewert wird hier NICHT stillschweigend zwischen "sauberem Erfolg" und "Abbruch mit bestmöglicher
  * Antwort" vermischt (siehe CCAF-Domäne 5.3 "Error Propagation in Multi-Agent Systems": Anti-Pattern "Silent Suppression" - ein Fehler, der wie ein normales Ergebnis aussieht, verhindert jede
  * sinnvolle Recovery-Entscheidung beim Aufrufer).
  *
  * @param text
  *   Der bestmöglich verfügbare Text - bei `complete = false` die Fehlermeldung des Abbruchs statt einer erfundenen Antwort.
  * @param complete
  *   `true`, falls der Model-Call regulär mit einer finalen Text-Antwort endete (kein Budget-/Transport-/Decode-Abbruch).
  * @param failureType
  *   `None` falls `complete`, sonst die Kategorie nach `AgentRunOutcome.classify` (siehe dort).
  */
final case class AgentRunOutcome(text: String, complete: Boolean, failureType: Option[FailureType])

object AgentRunOutcome:

  /** Ordnet einen Fehler (`AIGenException` aus kyo-ai oder unser eigener `Observability.BudgetExceeded`) einer groben `FailureType`-Kategorie zu. `AIDecodeException` (die Antwort entsprach nicht dem
    * verlangten Schema) gilt als `Validation`; alle anderen Fälle (Transport-Fehler, Timeout, Budget-Überschreitung, ausgeschöpfte Eval-Loop-Iterationen, fehlender API-Key) sind
    * Ressourcen-/Laufzeit-Grenzen, für die ein Retry (mit angepassten Parametern) grundsätzlich sinnvoll sein kann, daher einheitlich `Transient`.
    */
  def classify(error: AIGenException | Observability.BudgetExceeded): FailureType = error match
    case _: AIDecodeException => FailureType.Validation
    case _                    => FailureType.Transient

  /** Ersatz-Text für `Throwable.getMessage`, das bei manchen Exception-Typen `null` liefern kann - verhindert, dass `null` unerkannt über `AgentRunOutcome.text`/`rawOutputSnippet` in JSON/Prompts
    * einsickert (projektweite "kein `null`"-Konvention).
    */
  def safeMessage(error: Throwable): String = Option(error.getMessage).getOrElse(error.getClass.getSimpleName)

/** Ergebnis von `Agents.runStructured` - bündelt Model-Call-Ausgang UND Decode-Ausgang zu EINEM einheitlichen Fehlerbild, das `AgentFactResearcher`/`AgentRiskAnalyst` 1:1 auf ihr jeweiliges
  * Report-Format (`FactReport`/`RiskReport`) mappen (siehe dort) - vermeidet die andernfalls in beiden Worker-Agenten verdoppelte "nicht complete / nicht decodierbar / Erfolg"-Fallunterscheidung.
  *
  * @param parsed
  *   Das decodierte Ergebnis, `None` bei `status = PartialFailure`.
  */
final case class ReportOutcome[T](
    parsed: Option[T],
    status: ReportStatus,
    failureType: Option[FailureType],
    attemptedAction: Option[String],
    alternativeApproaches: List[String],
    rawOutputSnippet: Option[String],
)
