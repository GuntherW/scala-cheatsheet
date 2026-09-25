package agents

import kyo.*

/** Reine Unit-Tests für die Bausteine aus `Observability.scala` - bewusst OHNE echten API-Call/Netzwerk: `UsageCollector`/`Pricing` sind reine Datenstrukturen, und der Budget-Guard-Mechanismus
  * (`Abort.when`) lässt sich direkt mit `Abort.run(...).eval` prüfen - ganz ohne einen echten `AI.gen`/`LLM.run`-Aufruf.
  */
class ObservabilityTest extends munit.FunSuite:

  private def usage(input: Long, output: Long): AIStats = AIStats(input, Maybe.Absent, output, Maybe.Absent, turns = 1)

  test("UsageCollector.report summiert über mehrere Agenten (Caller) und markiert Modelle ohne Preiseintrag") {
    val collector = new Observability.UsageCollector
    collector.record(Observability.CallReport("Agent-A", "claude-sonnet-4-5", usage(1_000_000, 0)))
    collector.record(Observability.CallReport("Agent-B", "unbekanntes-modell", usage(1_000_000, 0)))

    val pricing = Observability.Pricing(Map("claude-sonnet-4-5" -> (BigDecimal(3), BigDecimal(15))))
    val report  = collector.report(pricing)

    assert(report.contains("Agent-A"))
    assert(report.contains("Agent-B"))
    assert(report.contains("unbekanntes-modell"), "Hinweis auf fehlenden Preiseintrag sollte im Report auftauchen")
  }

  test("UsageCollector sammelt Tokens über mehrere Calls hinweg") {
    val collector = new Observability.UsageCollector
    collector.record(Observability.CallReport("Test-Agent", "m", usage(100, 20)))
    collector.record(Observability.CallReport("Test-Agent", "m", usage(50, 10)))

    val calls = collector.snapshot()
    assertEquals(calls.size, 2)
    assertEquals(calls.map(_.usage.inputTokens).sum, 150L)
    assertEquals(calls.map(_.usage.outputTokens).sum, 30L)
  }

  test("Pricing.table enthält einen Preiseintrag für das projekteigene Router-Modell") {
    assert(Observability.Pricing.table.knows(LlmConfig.model))
  }

  test("Budget-Guard (Abort.when) loest erst bei Ueberschreitung des Limits aus") {
    val underBudget = Abort.run[Observability.BudgetExceeded](Abort.when(50L > 100L)(Observability.BudgetExceeded("Test", 50L))).eval
    assertEquals(underBudget, Result.Success(()))

    Abort.run[Observability.BudgetExceeded](Abort.when(150L > 100L)(Observability.BudgetExceeded("Test", 150L))).eval match
      case Result.Failure(Observability.BudgetExceeded(caller, spent)) =>
        assertEquals(caller, "Test")
        assertEquals(spent, 150L)
      case other                                                       => fail(s"erwartete Failure(BudgetExceeded), bekam: $other")
  }
