package agents

import sttp.ai.claude.models.ClaudeModel
import sttp.ai.core.agent.*
import sttp.ai.core.agent.interceptor.{BudgetInterceptor, LogLevel, LoggingInterceptor, ModelPrice, PriceTable}
import sttp.monad.IdentityMonad
import sttp.shared.Identity

import java.util.concurrent.ConcurrentLinkedQueue
import scala.jdk.CollectionConverters.*

private given sttp.monad.MonadError[Identity] = IdentityMonad

/** Bausteine rund um `sttp.ai.core.agent.AgentInterceptor` - dem Middleware-Konzept von sttp-ai für den Agent-Loop (siehe [[https://sttp-ai.softwaremill.com/agents/interceptors.html]]): Interceptoren
  * umschließen Iterationen, LLM-Calls und Tool-Aufrufe "onion-style" (wie sttp-Backend-Wrapper) und können anhand des vom Provider gemeldeten Token-Verbrauchs (`AgentResponse.usage`) den Loop
  * vorzeitig, aber geordnet beenden (`LoopDecision.FinishNow`).
  *
  * Dieses Projekt nutzt drei Interceptoren pro Agent (siehe `AnthropicClient.buildAgent`):
  *   1. `LoggingInterceptor` (aus sttp-ai) - ersetzt das frühere handgeschriebene `[LLM:<Aufrufer>] ...`-Logging.
  *   1. `UsageTrackingInterceptor` (dieses Projekt) - sammelt Tokens/Kosten/Dauer pro LLM-Call in einem zentralen, pipeline-weiten `UsageCollector`, damit am Ende der gesamten Pipeline (nicht nur pro
  *      Agent) eine Gesamtübersicht möglich ist.
  *   1. `BudgetInterceptor` (aus sttp-ai) - generöses Token-Limit pro Agent-Aufruf als Sicherheitsnetz (siehe `Pricing.perCallTokenBudget`).
  */
object Interceptors:

  private def truncate(s: String, maxLen: Int = 300): String =
    val flattened = Option(s).getOrElse("").replaceAll("\\s+", " ").trim
    if flattened.length > maxLen then flattened.take(maxLen) + "…" else flattened

  /** `LoggingInterceptor` mit einem an das bisherige Format (`[LLM:<Aufrufer>] ...`) angelehnten Sink. Loggt gröber als das alte, handgeschriebene `AnthropicClient.log` (keine einzelnen
    * Text-/Tool-Content-Previews mehr), dafür "for free" inklusive Iterations-Grenzen und Tool-Fehlern - beides fehlte im alten Logging komplett.
    */
  def loggingFor(caller: String): LoggingInterceptor[Identity] =
    LoggingInterceptor[Identity] { (level, msg) =>
      val tag = level match
        case LogLevel.Debug => "debug"
        case LogLevel.Info  => "info"
        case LogLevel.Warn  => "WARN"
      println(s"[LLM:$caller/$tag] ${truncate(msg)}")
    }

  /** Ein einzelner protokollierter LLM-Call (eine Iteration eines Agent-Loops), gesammelt von [[UsageTrackingInterceptor]]. */
  final case class CallReport(
      caller: String,
      iteration: Int,
      model: Option[String],
      usage: TokenUsage,
      elapsedSeconds: Double,
  )

  /** Pipeline-weite, nebenläufigkeitssichere Sammelstelle für [[CallReport]]s.
    *
    * Wird bewusst außerhalb des sttp-ai-Interceptor-Mechanismus gehalten: `AgentRunState`/Budgets sind pro `Agent.run`-Aufruf "stage-local" (siehe sttp-ai-Doku zu `andThen`), aber der Orchestrator
    * führt mehrere, teils parallele (`ox.par`) Agent-Aufrufe pro Pipeline-Lauf aus - für eine Gesamtübersicht über Tokens/Kosten aller Agenten (inkl. Planner) braucht es daher eine gemeinsame,
    * Thread-sichere Sammelstelle, die von jedem einzelnen `UsageTrackingInterceptor` befüllt wird.
    */
  final class UsageCollector:
    private val reports = new ConcurrentLinkedQueue[CallReport]()

    def record(report: CallReport): Unit = reports.add(report)

    def snapshot(): List[CallReport] = reports.asScala.toList

    /** Menschenlesbarer Abschlussbericht: Gesamt-Tokens/-Kosten, Aufschlüsselung pro Agent, sowie ein Hinweis auf Modelle ohne Preiseintrag in `priceTable` (dort ist die berechnete Kosten-Komponente
      * `0`, siehe `PriceTable.costOf` in sttp-ai - das ist bewusst so, damit ein Kostenbudget bei unbekannten Modellen nicht fälschlich fehlschlägt, kann hier aber leicht übersehen werden).
      */
    def report(priceTable: PriceTable): String =
      val all = snapshot()
      if all.isEmpty then "[Usage] Keine LLM-Calls aufgezeichnet."
      else
        val byCaller       = all.groupBy(_.caller).toList.sortBy(_._1)
        val perCallerLines = byCaller.map { case (caller, calls) =>
          val usage    = calls.map(_.usage).reduce(_ + _)
          val cost     = priceTable.costOf(calls.map(c => LlmCallUsage(c.model, c.usage)))
          val duration = calls.map(_.elapsedSeconds).sum
          f"  - $caller%-16s calls=${calls.size}%2d  input=${usage.inputTokens.value}%6d  output=${usage.outputTokens.value}%6d  " +
            f"total=${usage.totalTokens.value}%6d  ~${duration}%.1fs  ~${cost.value}%.4f USD"
        }

        val totalUsage = all.map(_.usage).reduce(_ + _)
        val totalCost  = priceTable.costOf(all.map(c => LlmCallUsage(c.model, c.usage)))

        val unknownModels = all.flatMap(_.model).distinct.filterNot(priceTable.prices.contains).sorted
        val unknownHint   =
          if unknownModels.isEmpty then Nil
          else List(s"  Hinweis: kein Preiseintrag für Modell(e) ${unknownModels.mkString(", ")} - Kostenanteil dafür ist 0, siehe Pricing.table.")

        (List(
          "[Usage] Zusammenfassung über alle Agenten/Planner-Aufrufe dieser Pipeline:",
        ) ++ perCallerLines ++ List(
          f"  TOTAL            calls=${all.size}%2d  input=${totalUsage.inputTokens.value}%6d  output=${totalUsage.outputTokens.value}%6d  " +
            f"total=${totalUsage.totalTokens.value}%6d  ~${totalCost.value}%.4f USD",
        ) ++ unknownHint).mkString("\n")

  /** Schreibt jeden LLM-Call eines Agent-Loops (`aroundLlmCall`) in einen gemeinsamen [[UsageCollector]] - Kern der "Kosten/Tokens für Lernzwecke sichtbar machen"-Anforderung dieses Projekts. Reine
    * Beobachtung (kein `decide`/Budget - das übernimmt `BudgetInterceptor`).
    */
  final class UsageTrackingInterceptor(caller: String, collector: UsageCollector) extends AgentInterceptor[Identity]:
    override def aroundLlmCall(ctx: LlmCallContext)(next: => Identity[AgentResponse]): Identity[AgentResponse] =
      val start    = System.nanoTime()
      val response = next
      val elapsed  = (System.nanoTime() - start) / 1e9
      collector.record(CallReport(caller, ctx.iterationInfo.iteration, response.model, response.usage.getOrElse(TokenUsage.Zero), elapsed))
      response

  /** Näherungsweise, öffentlich bekannte Anthropic-Listenpreise (USD pro Million Tokens, Stand der Modell-Generationen) - '''keine''' verbindlichen Preise des in diesem Projekt genutzten Routers
    * (`router.eu.requesty.ai`), der abweichend abrechnen kann und dessen `model`-Feld in der Antwort ggf. nicht exakt einer der hier gelisteten Anthropic-Modell-Ids entspricht. Nach dem ersten echten
    * Lauf (siehe `UsageCollector.report`, Abschnitt "Hinweis: kein Preiseintrag ...") ggf. um die tatsächlich gemeldete Modell-Id ergänzen.
    */
  object Pricing:

    private def haiku(id: String)  = id -> ModelPrice(inputPerMTok = BigDecimal("1.00"), outputPerMTok = BigDecimal("5.00"))
    private def sonnet(id: String) = id -> ModelPrice(inputPerMTok = BigDecimal("3.00"), outputPerMTok = BigDecimal("15.00"))
    private def opus(id: String)   = id -> ModelPrice(inputPerMTok = BigDecimal("15.00"), outputPerMTok = BigDecimal("75.00"))

    private def guessPrice(modelId: String): (String, ModelPrice) =
      val lower = modelId.toLowerCase
      if lower.contains("haiku") then haiku(modelId)
      else if lower.contains("opus") then opus(modelId)
      else sonnet(modelId) // Sonnet als Default-Annahme, da dieses Projekt standardmäßig ein Sonnet-Modell nutzt (siehe Agent.scala)

    /** Preistabelle für alle bekannten `ClaudeModel`-Ids '''plus''' den in diesem Projekt via Router genutzten Modell-String (`"vertex/claude-sonnet-5@eu"`, siehe `Agent.scala`) - Letzterer ist eine
      * Schätzung (Sonnet-Tarif), bis ein echter Lauf zeigt, welche Modell-Id der Router tatsächlich zurückmeldet.
      */
    val table: PriceTable = PriceTable(
      (ClaudeModel.values.toList.map(_.value) :+ "vertex/claude-sonnet-5@eu").map(guessPrice).toMap
    )

    /** Generöses, pro Agent-Aufruf geltendes Token-Limit als Sicherheitsnetz (siehe `BudgetInterceptor` in sttp-ai) - soll im Normalfall nie greifen, demonstriert aber den "graceful stop"-Mechanismus
      * (`LoopDecision.FinishNow`/`FinishReason.BudgetExceeded`) statt eines harten Fehlers, falls ein Agent doch einmal außer Kontrolle gerät (z. B. Endlos-Tool-Use-Loop).
      */
    val perCallTokenBudget: Tokens = Tokens(200_000L)

  def budgetSafetyNet: BudgetInterceptor[Identity] = BudgetInterceptor[Identity](maxTotalTokens = Some(Pricing.perCallTokenBudget))
