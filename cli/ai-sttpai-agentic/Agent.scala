package agents

import io.circe.{Codec, Decoder, Encoder}
import sttp.ai.core.agent.{AgentFailure, AgentIncomplete, AgentTool}
import sttp.shared.Identity

/** Grobe Fehlerkategorie nach dem CCAF-5.3-Schema ("Error Propagation in Multi-Agent Systems"): `Transient` (Budget/Token-Limit/Max-Iterations erreicht - ein Retry mit angepassten Parametern, z. B.
  * höherem `maxTokens` oder engerem Scope, könnte helfen) oder `Validation` (der Model-Call selbst lief sauber durch, aber die Antwort entsprach nicht dem verlangten JSON-Schema). Wird sowohl von
  * `Agent.run` (technischer Abbruch, siehe `AgentRunOutcome`) als auch von `Agent.runStructured` (JSON-Parsing-Fehler, siehe `ReportOutcome`) genutzt - aus Coordinator-Sicht (`AgentSynthesis`) sind
  * beides gleichwertige "partial_failure"-Gründe.
  *
  * Serialisiert bewusst als schlichter lowercase-String (`"transient"`/`"validation"`, via eigenem `Codec`) statt als verschachteltes `{"FailureType": "Transient"}` - das flache JSON-Schema wird 1:1
  * im System-Prompt von `AgentSynthesis` dokumentiert und muss dazu passen.
  */
enum FailureType:
  case Transient, Validation

  def wireValue: String = this match
    case FailureType.Transient  => "transient"
    case FailureType.Validation => "validation"

object FailureType:
  given Codec[FailureType] = Codec.from(
    Decoder.decodeString.emap(s => FailureType.values.find(_.wireValue == s).toRight(s"Ungültiger failureType: '$s'")),
    Encoder.encodeString.contramap(_.wireValue),
  )

/** Status eines strukturierten Worker-Reports (`FactReport`/`RiskReport`, siehe `ReportOutcome`) - `"success"` (auch bei leeren `facts`/`risks`, siehe dort) oder `"partial_failure"`. Gleiches
  * Wire-Format-Prinzip wie `FailureType` (siehe dort für die Begründung).
  */
enum ReportStatus:
  case Success, PartialFailure

  def wireValue: String = this match
    case ReportStatus.Success        => "success"
    case ReportStatus.PartialFailure => "partial_failure"

object ReportStatus:
  given Codec[ReportStatus] = Codec.from(
    Decoder.decodeString.emap(s => ReportStatus.values.find(_.wireValue == s).toRight(s"Ungültiger status: '$s'")),
    Encoder.encodeString.contramap(_.wireValue),
  )

/** Ergebnis eines `Agent.run`-Aufrufs - im Gegensatz zum früheren, reinen `String`-Rückgabewert wird hier NICHT mehr stillschweigend zwischen "sauberem Erfolg" und "Abbruch mit bestmöglicher Antwort"
  * vermischt (siehe CCAF-Domäne 5.3 "Error Propagation in Multi-Agent Systems": Anti-Pattern "Silent Suppression" - ein Fehler, der wie ein normales Ergebnis aussieht, verhindert jede sinnvolle
  * Recovery-Entscheidung beim Aufrufer).
  *
  * @param text
  *   Der bestmöglich verfügbare Text - bei `complete = false` ggf. unvollständig/abgeschnitten (`AgentFailure.rawAnswer`).
  * @param complete
  *   `true`, falls der Model-Call regulär mit einer finalen Text-Antwort endete (kein Budget-/Token-/Iterations-Abbruch).
  * @param failureType
  *   `None` falls `complete`, sonst die Kategorie nach `AgentRunOutcome.classify` (siehe dort).
  */
final case class AgentRunOutcome(text: String, complete: Boolean, failureType: Option[FailureType])

private object AgentRunOutcome:

  /** Ordnet eine `AgentFailure` (siehe sttp-ai `AgentResult.scala`) einer groben `FailureType`-Kategorie zu. Da `Agent.run` intern IMMER mit `Out = String` arbeitet (kein Structured Output), kann das
    * generische Output-Parsing von sttp-ai (`LoopAgent.parseOutput`) nie fehlschlagen - `AgentParseError` sollte hier praktisch nie auftreten und wird defensiv als `Validation` eingeordnet.
    * `AgentIncomplete` (der eigentlich relevante Fall) entsteht bei `MaxIterations`/`TokenLimit`/`BudgetExceeded`/`Custom`-Forced-Stop/`Error` - allesamt Ressourcen-/Laufzeit-Grenzen, für die ein
    * Retry (mit angepassten Parametern) grundsätzlich sinnvoll sein kann, daher einheitlich `Transient`.
    */
  def classify(failure: AgentFailure): FailureType = failure match
    case _: AgentIncomplete => FailureType.Transient
    case _                  => FailureType.Validation

/** Ergebnis von `Agent.runStructured` - bündelt Model-Call-Ausgang UND JSON-Parsing-Ausgang zu EINEM einheitlichen Fehlerbild, das `AgentFactResearcher`/`AgentRiskAnalyst` 1:1 auf ihr jeweiliges
  * Report-Format (`FactReport`/`RiskReport`) mappen (siehe dort) - vermeidet die andernfalls in beiden Worker-Agenten verdoppelte "nicht complete / nicht parsebar / Erfolg"-Fallunterscheidung.
  *
  * @param parsed
  *   Das geparste Ergebnis, `None` bei `status = PartialFailure`.
  */
final case class ReportOutcome[T](
    parsed: Option[T],
    status: ReportStatus,
    failureType: Option[FailureType],
    attemptedAction: Option[String],
    alternativeApproaches: List[String],
    rawOutputSnippet: Option[String],
)

/** Basisklasse für alle Agenten im Multi-Agenten-System.
  *
  * Terminologie (wichtig für die CCAF-Zertifizierung):
  *
  *   - '''Agent''': Eine eigenständige Einheit, die einen LLM-Aufruf (Model Call) mit einem spezifischen System-Prompt (Rolle) kapselt und eine klar abgegrenzte Aufgabe löst.
  *   - '''System Prompt''': Instruktion, die dem Modell VOR der eigentlichen Aufgabe mitgegeben wird und dessen Rolle, Ton und Einschränkungen definiert.
  *   - '''Tool Use / Function Calling''': Fähigkeit des Modells, definierte externe Werkzeuge aufzurufen, um Informationen zu beschaffen oder Aktionen auszuführen, die es selbst nicht "weiß"/kann.
  *     Zwei Arten: * Server-seitig (z. B. `web_search`): wird komplett vom Anthropic-Server ausgeführt, ein einzelner Request genügt. * Client-seitig / custom (z. B. `calculate_tco`): das Modell
  *     liefert nur den Aufrufwunsch zurück (`stop_reason == "tool_use"`), WIR führen die Funktion aus und senden das Ergebnis zurück (Multi-Turn-Loop).
  *   - '''Orchestrator''': Die übergeordnete Steuerungslogik, die entscheidet, welche Agenten wann aufgerufen werden (sequentiell oder parallel) und wie deren Ergebnisse zusammengeführt werden.
  *   - '''Worker Agent''': Ein Agent, der Teilaufgaben eigenständig und (meist) parallel zu anderen Workern bearbeitet, ohne von deren Zwischenergebnissen zu wissen.
  *   - '''Synthesis / Aggregator Agent''': Ein Agent, der die Ausgaben mehrerer Worker-Agenten als Kontext erhält und daraus ein konsolidiertes Endergebnis erzeugt.
  *   - '''Interceptor''': sttp-ai-Middleware um den Agent-Loop (Logging, Usage-Tracking, Budgets - siehe `Interceptors.scala`), onion-style um jede Iteration/jeden LLM-Call/Tool-Aufruf gelegt (siehe
  *     `AnthropicClient.buildAgent`).
  *
  * Der Model-Call selbst läuft immer über `AnthropicClient.buildAgent`, das intern - für `Agent.scala` transparent - entweder den generischen, client-seitigen Tool-Use-Loop von sttp-ai
  * (`ClaudeAgent.synchronous`) oder einen bewusst simplen Einzel-Request-Pfad für das server-seitige `web_search`-Tool wählt (siehe `AnthropicClient.buildAgent`/`webSearchAgent` für die Begründung) -
  * statt des früheren, direkt in diesem Projekt handgeschriebenen Tool-Use-Loops -, inkl. Logging-/Usage-/Budget-Interceptoren.
  */
abstract class Agent(
    val name: String,
    val systemPrompt: String,
    val useWebSearch: Boolean = false,
    val clientTools: Seq[AgentTool[Identity, ?]] = Seq.empty,
    val model: String = "vertex/claude-sonnet-5@eu",
):

  require(
    !(useWebSearch && clientTools.nonEmpty),
    "web_search laesst sich nicht mit client-seitigen Tools kombinieren (siehe README, Abschnitt zu Tool-Typen) - " +
      "der Einzel-Request-Pfad fuer web_search bietet dafuer keinen Tool-Use-Loop an.",
  )

  /** Einheitlich präfixtes Warn-Logging (`[LLM:<Aufrufer>/WARN] ...`) für alle "unsauberen, aber nicht harten" Zustände dieses Agenten (Model-Call-Abbruch in `run`, JSON-Parsing-Fehler in
    * `runStructured`, Synthese-Abbruch in `AgentSynthesis`) - ein zentraler Ort statt mehrfach dupliziertem `println(s"[LLM:$name/WARN] ...")`.
    */
  protected def warn(message: String): Unit = println(s"[LLM:$name/WARN] $message")

  /** Führt den Model-Call über `AnthropicClient.buildAgent` aus - EIN einheitlicher Aufruf für beide Tool-Typen (server-/client-seitig, bewusst exklusiv, siehe `require` oben); welcher der beiden
    * Pfade intern läuft, entscheidet `AnthropicClient.buildAgent` selbst anhand von `useWebSearch`.
    *
    * Bei einem "unsauberen" Ende (z. B. Budget-Interceptor greift, `maxTokens` erreicht, oder - nur beim Tool-Use-Loop - maximale Iterationen erreicht, bevor eine finale Text-Antwort ohne Tool-Aufruf
    * kam) wird die bestmögliche verfügbare Antwort (`AgentFailure.rawAnswer`) NICHT mehr wie ein regulärer Erfolg zurückgegeben (siehe `AgentRunOutcome`-Scaladoc, CCAF 5.3 "Silent Suppression") - der
    * Aufrufer bekommt über `AgentRunOutcome.complete`/`failureType` explizit mitgeteilt, dass es sich um ein unvollständiges Ergebnis handelt, und kann selbst entscheiden, wie er damit umgeht.
    */
  protected def run(userMessage: String, maxTokens: Int = 2000): AgentRunOutcome =
    val agent = AnthropicClient.buildAgent(
      caller = name,
      model = model,
      systemPrompt = systemPrompt,
      tools = clientTools,
      useWebSearch = useWebSearch,
      maxTokens = Some(maxTokens),
    )
    agent.run(userMessage)(AnthropicClient.backend).finalAnswer match
      case Right(answer) => AgentRunOutcome(answer, complete = true, failureType = None)
      case Left(failure) =>
        val failureType = AgentRunOutcome.classify(failure)
        warn(s"Kein regulärer Abschluss des Model-Calls (${failure.getClass.getSimpleName}, failureType=${failureType.wireValue}) - liefere unvollständiges Ergebnis statt es zu verschleiern.")
        AgentRunOutcome(failure.rawAnswer, complete = false, failureType = Some(failureType))

  /** Wie `run`, aber zusätzlich mit dem Versuch, die Text-Antwort als `T` zu parsen (`JsonExtraction.parseLenient`) - vereinheitlicht die Fehlerbehandlung für Worker-Agenten, die strukturierte
    * JSON-Reports statt Freitext liefern (siehe `AgentFactResearcher.research`/`AgentRiskAnalyst.analyze`, CCAF 5.3 "Error Propagation"). Ohne diese Bündelung müsste jeder Worker-Agent dieselbe
    * dreistufige Fallunterscheidung (nicht `complete` / JSON nicht parsebar / Erfolg) separat implementieren.
    *
    * @param attemptedAction
    *   Freitext-Beschreibung dessen, was versucht wurde - landet nur bei `status = PartialFailure` im `ReportOutcome` (siehe dort).
    */
  protected def runStructured[T: Decoder](userMessage: String, attemptedAction: String, maxTokens: Int = 2000): ReportOutcome[T] =
    val outcome = run(userMessage, maxTokens)
    if !outcome.complete then
      ReportOutcome(
        parsed = None,
        status = ReportStatus.PartialFailure,
        failureType = outcome.failureType,
        attemptedAction = Some(attemptedAction),
        alternativeApproaches = List(
          "Erneut mit engerem Themen-Scope versuchen",
          "run(..., maxTokens = ...) mit höherem Limit erneut aufrufen",
        ),
        rawOutputSnippet = Some(outcome.text),
      )
    else
      JsonExtraction.parseLenient[T](outcome.text) match
        case Right(parsed) => ReportOutcome(Some(parsed), ReportStatus.Success, None, None, List.empty, None)
        case Left(error)   =>
          warn(s"Konnte JSON-Antwort nicht parsen ($error) - liefere partial_failure statt ein erfundenes Ergebnis vorzutäuschen.")
          ReportOutcome(
            parsed = None,
            status = ReportStatus.PartialFailure,
            failureType = Some(FailureType.Validation),
            attemptedAction = Some(attemptedAction),
            alternativeApproaches = List("Erneut anfragen - Modell hat sich nicht ans verlangte JSON-Schema gehalten."),
            rawOutputSnippet = Some(outcome.text),
          )
