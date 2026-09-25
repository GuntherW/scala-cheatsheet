package agents

import sttp.ai.core.agent.*
import sttp.ai.core.agent.interceptor.{BudgetInterceptor, ModelPrice, PriceTable}
import sttp.monad.IdentityMonad
import sttp.shared.Identity

/** Reine Unit-Tests für die Bausteine aus `Interceptors.scala` - bewusst OHNE echten API-Call/Netzwerk: `AgentInterceptor.aroundLlmCall` lässt sich direkt mit einer selbstgebauten `AgentResponse`
  * aufrufen (siehe Kommentar in `UsageTrackingInterceptor`), `BudgetInterceptor` lässt sich direkt über `AgentRunState`/`decide` prüfen - beides ganz ohne den vollen
  * `sttp.ai.core.agent.LoopAgent`-Loop.
  */
class InterceptorsTest extends munit.FunSuite:

  private given sttp.monad.MonadError[Identity] = IdentityMonad

  private def usage(input: Long, output: Long): TokenUsage =
    TokenUsage(inputTokens = Tokens(input), outputTokens = Tokens(output), cachedInputTokens = Tokens.Zero, reasoningTokens = Tokens.Zero)

  private def llmCallContext(iteration: Int): LlmCallContext =
    LlmCallContext(ConversationHistory.empty, includeTools = true, IterationInfo(iteration, maxIterations = 10))

  test("UsageTrackingInterceptor sammelt Tokens über mehrere LLM-Calls hinweg im geteilten UsageCollector") {
    val collector   = new Interceptors.UsageCollector
    val interceptor = new Interceptors.UsageTrackingInterceptor("Test-Agent", collector)
    val response1   = AgentResponse("Zwischenschritt", Seq.empty, StopReason.ToolUse, usage = Some(usage(100, 20)), model = Some("claude-sonnet-4-5-20250929"))
    val response2   = AgentResponse("Fertig", Seq.empty, StopReason.EndTurn, usage = Some(usage(50, 10)), model = Some("claude-sonnet-4-5-20250929"))

    interceptor.aroundLlmCall(llmCallContext(1))(response1)
    interceptor.aroundLlmCall(llmCallContext(2))(response2)

    val calls      = collector.snapshot()
    assertEquals(calls.size, 2)
    assertEquals(calls.map(_.caller).distinct, List("Test-Agent"))
    val totalUsage = calls.map(_.usage).reduce(_ + _)
    assertEquals(totalUsage.inputTokens.value, 150L)
    assertEquals(totalUsage.outputTokens.value, 30L)
  }

  test("UsageCollector.report summiert über mehrere Agenten (Caller) und markiert Modelle ohne Preiseintrag") {
    val collector = new Interceptors.UsageCollector
    collector.record(Interceptors.CallReport("Agent-A", 1, Some("claude-sonnet-4-5-20250929"), usage(1_000_000, 0), 1.0))
    collector.record(Interceptors.CallReport("Agent-B", 1, Some("unbekanntes-modell"), usage(1_000_000, 0), 1.0))

    val priceTable = PriceTable(Map("claude-sonnet-4-5-20250929" -> ModelPrice(inputPerMTok = BigDecimal(3), outputPerMTok = BigDecimal(15))))
    val report     = collector.report(priceTable)

    assert(report.contains("Agent-A"))
    assert(report.contains("Agent-B"))
    assert(report.contains("unbekanntes-modell"), "Hinweis auf fehlenden Preiseintrag sollte im Report auftauchen")
  }

  test("Pricing.table enthält für jedes ClaudeModel und den projekteigenen Router-Modellstring einen Preiseintrag") {
    import sttp.ai.claude.models.ClaudeModel
    ClaudeModel.values.foreach(m => assert(Interceptors.Pricing.table.prices.contains(m.value), s"kein Preis für ${m.value}"))
    assert(Interceptors.Pricing.table.prices.contains("vertex/claude-sonnet-5@eu"))
  }

  test("BudgetInterceptor.decide beendet den Loop erst, wenn das Token-Budget erreicht ist") {
    val budget = BudgetInterceptor[Identity](maxTotalTokens = Some(Tokens(100L)))

    val underBudget = AgentRunState(iterationsCompleted = 1, maxIterations = 10, usage = usage(30, 20), llmCalls = Seq.empty)
    assertEquals(budget.decide(underBudget), LoopDecision.Continue)

    val overBudget = AgentRunState(iterationsCompleted = 2, maxIterations = 10, usage = usage(80, 30), llmCalls = Seq.empty)
    budget.decide(overBudget) match
      case LoopDecision.FinishNow(FinishReason.BudgetExceeded, _) => () // erwartet
      case other                                                  => fail(s"erwartete FinishNow(BudgetExceeded), bekam: $other")
  }
