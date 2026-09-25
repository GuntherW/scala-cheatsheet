package agents

import kyo.*

import java.util.concurrent.ConcurrentLinkedQueue
import scala.jdk.CollectionConverters.*

/** Ersatz für den Interceptor-Stack des sttp-ai-Originals (`Interceptors.scala`: `LoggingInterceptor`/`UsageTrackingInterceptor`/`BudgetInterceptor`) auf Basis der `kyo-ai`-Enablements `Observe`
  * (wire-tier Turn-Benachrichtigung, siehe kyo-ai-README Abschnitt "Tracking usage") und `kyo.Log`.
  *
  * Drei Bausteine pro Agenten-Aufruf (siehe `Agents.withObservability`):
  *   1. Ein `Observe`, das jeden abgeschlossenen Model-Turn als `[LLM:<Aufrufer>] ...`-Zeile loggt (Ersatz für `LoggingInterceptor`).
  *   1. Ein `Observe`, das jeden Turn in einem pipeline-weiten, nebenläufigkeitssicheren `UsageCollector` sammelt (Ersatz für `UsageTrackingInterceptor`).
  *   1. Ein `Observe`, das über einen `AtomicRef` die Tokens EINES Agenten-Aufrufs aufsummiert und bei Überschreitung eines Limits `Abort.when(...)` auslöst (Ersatz für `BudgetInterceptor`, Pattern
  *      aus dem kyo-ai-README, Abschnitt "Tracking usage" / Beispiel `capped`) - ein generöses Sicherheitsnetz, das im Normalfall nie greifen soll.
  */
object Observability:

  /** Wird ausgelöst, falls ein einzelner Agenten-Aufruf (siehe `Agents.run`) das Token-Budget aus `budgetTokens` überschreitet - Analogon zu `FinishReason.BudgetExceeded` im sttp-ai-Original, hier
    * aber als eigener, expliziter Fehlertyp statt einer `LoopDecision`, da kyo-ai keinen vergleichbaren "graceful stop"-Mechanismus für Enablements kennt.
    */
  final case class BudgetExceeded(caller: String, spentTokens: Long)

  /** Ein einzelner protokollierter Model-Turn, gesammelt vom `UsageCollector`. */
  final case class CallReport(caller: String, model: String, usage: AIStats)

  /** Pipeline-weite, nebenläufigkeitssichere Sammelstelle für `CallReport`s - analog zu `Interceptors.UsageCollector` im Original: Der Orchestrator führt mehrere, teils parallele (`Async.foreach`)
    * Agenten-Aufrufe pro Pipeline-Lauf aus, für eine Gesamtübersicht über Tokens/Kosten aller Agenten (inkl. Planner) braucht es daher eine gemeinsame, Thread-sichere Sammelstelle.
    */
  final class UsageCollector:
    private val reports = new ConcurrentLinkedQueue[CallReport]()

    def record(report: CallReport): Unit = { val _ = reports.add(report) }

    def snapshot(): List[CallReport] = reports.asScala.toList

    final private case class Totals(stats: AIStats, cost: BigDecimal)

    private def totalsOf(calls: List[CallReport], pricing: Pricing): Totals =
      calls.foldLeft(Totals(AIStats.empty, BigDecimal(0))) { case (acc, c) =>
        Totals(acc.stats.add(c.usage), acc.cost + pricing.costOf(c.model, c.usage))
      }

    /** Menschenlesbarer Abschlussbericht: Gesamt-Tokens/-Kosten, Aufschlüsselung pro Agent, sowie ein Hinweis auf Modelle ohne Preiseintrag in `pricing` (die berechnete Kosten-Komponente ist dann
      * bewusst `0`, damit ein unbekanntes Modell den Report nicht hart scheitern lässt).
      */
    def report(pricing: Pricing): String =
      val all = snapshot()
      if all.isEmpty then "[Usage] Keine LLM-Calls aufgezeichnet."
      else
        val byCaller       = all.groupBy(_.caller).toList.sortBy(_._1)
        val perCallerLines = byCaller.map { case (caller, calls) =>
          val totals = totalsOf(calls, pricing)
          val input  = totals.stats.inputTokens
          val output = totals.stats.outputTokens
          f"  - $caller%-16s calls=${calls.size}%2d  input=$input%6d  output=$output%6d  total=${input + output}%6d  ~${totals.cost}%.4f USD"
        }

        val totals      = totalsOf(all, pricing)
        val totalInput  = totals.stats.inputTokens
        val totalOutput = totals.stats.outputTokens
        val totalCost   = totals.cost

        val unknownModels = all.map(_.model).distinct.filterNot(pricing.knows).sorted
        val unknownHint   =
          if unknownModels.isEmpty then Nil
          else List(s"  Hinweis: kein Preiseintrag für Modell(e) ${unknownModels.mkString(", ")} - Kostenanteil dafür ist 0, siehe Pricing.table.")

        (List("[Usage] Zusammenfassung über alle Agenten/Planner-Aufrufe dieser Pipeline:") ++ perCallerLines ++ List(
          f"  TOTAL            calls=${all.size}%2d  input=$totalInput%6d  output=$totalOutput%6d  total=${totalInput + totalOutput}%6d  ~$totalCost%.4f USD",
        ) ++ unknownHint).mkString("\n")

  /** Näherungsweise, öffentlich bekannte Anthropic-Listenpreise (USD pro Million Tokens) - '''keine''' verbindlichen Preise des in diesem Projekt genutzten Routers (`router.eu.requesty.ai`), der
    * abweichend abrechnen kann. Analog zu `Interceptors.Pricing` im Original.
    */
  final case class Pricing(prices: Map[String, (BigDecimal, BigDecimal)]):
    def knows(model: String): Boolean                     = prices.contains(model)
    def costOf(model: String, usage: AIStats): BigDecimal =
      prices.get(model) match
        case Some((inPerMTok, outPerMTok)) =>
          (BigDecimal(usage.inputTokens) * inPerMTok + BigDecimal(usage.outputTokens) * outPerMTok) / BigDecimal(1_000_000)
        case None                          => BigDecimal(0)

  object Pricing:
    private def haiku(id: String)  = id -> (BigDecimal("1.00"), BigDecimal("5.00"))
    private def sonnet(id: String) = id -> (BigDecimal("3.00"), BigDecimal("15.00"))
    private def opus(id: String)   = id -> (BigDecimal("15.00"), BigDecimal("75.00"))

    private def guessPrice(modelId: String): (String, (BigDecimal, BigDecimal)) =
      val lower = modelId.toLowerCase
      if lower.contains("haiku") then haiku(modelId)
      else if lower.contains("opus") then opus(modelId)
      else sonnet(modelId) // Sonnet als Default-Annahme, da dieses Projekt standardmäßig ein Sonnet-Modell nutzt (siehe LlmConfig.scala)

    /** Preistabelle für das in diesem Projekt via Router genutzte Modell (`LlmConfig.model`) - eine Schätzung (Sonnet-Tarif), bis ein echter Lauf zeigt, welche Modell-Id der Router tatsächlich
      * zurückmeldet.
      */
    val table: Pricing = Pricing(Map(guessPrice(LlmConfig.model)))

  private val collector = new UsageCollector

  /** Zugriff auf den pipeline-weiten `UsageCollector` (analog zu `AnthropicClient.usageCollector` im Original) - befüllt von `Agents.withObservability`, ausgelesen von `Orchestrator.runPipeline` am
    * Ende einer Pipeline.
    */
  def usageCollector: UsageCollector = collector

  private def loggingObserver(caller: String): Observe[Any] =
    Observe.init { (_, reply) =>
      AI.config.map(c => Log.info(s"[LLM:$caller] model=${c.modelName} tokens=${reply.usage.totalTokens} stop=${reply.stopReason}"))
    }

  private def usageObserver(caller: String): Observe[Any] =
    Observe.init { (_, reply) =>
      AI.config.map(c => collector.record(CallReport(caller, c.modelName, reply.usage)))
    }

  /** Generöses, pro Agenten-Aufruf geltendes Token-Limit als Sicherheitsnetz - Analogon zu `Interceptors.Pricing.perCallTokenBudget`. */
  val perCallTokenBudget: Long = 200_000L

  /** Legt Logging-/Usage-/Budget-`Observe`s um `v` (siehe Klassendoc) - Ersatz für `AnthropicClient.commonInterceptors`. Der Budget-Guard lebt in einem eigenen `AtomicRef` pro Aufruf (nicht
    * pipeline-weit), analog zum `AgentRunState`-Scope des sttp-ai-`BudgetInterceptor`s (ein Sicherheitsnetz pro einzelnem Agenten-Aufruf, keine globale Pipeline-Bremse).
    */
  def withObservability[A, S](caller: String, budgetTokens: Long = perCallTokenBudget)(
      v: A < (LLM & S),
  ): Result[BudgetExceeded, A] < (LLM & S & Sync) =
    AtomicRef.init(0L).map { spent =>
      val budgetGuard = Observe.init { (_, reply) =>
        spent.updateAndGet(_ + reply.usage.totalTokens).map { total =>
          Abort.when(total > budgetTokens)(BudgetExceeded(caller, total))
        }
      }
      Abort.run[BudgetExceeded](AI.enable(loggingObserver(caller), usageObserver(caller), budgetGuard)(v))
    }
