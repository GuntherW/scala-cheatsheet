package agents

import ox.par

/** Reine Steuerlogik (kein LLM-Call): Fact-Researcher und Risk-Analyst laufen parallel (Fan-out, `ox.par`), danach erstellt der Synthesis-Agent den Bericht (Fan-in). */
object Orchestrator:

  final case class PipelineResult(facts: String, risks: String, report: String)

  def runPipeline(topic: String): PipelineResult =
    println(s"[Orchestrator] Starte Fact-Researcher und Risk-Analyst PARALLEL für: '$topic'")
    val start          = System.nanoTime()
    val (facts, risks) = par(AgentFactResearcher.research(topic), AgentRiskAnalyst.analyze(topic))
    println(f"[Orchestrator] Beide Worker fertig nach ${(System.nanoTime() - start) / 1e9}%.1fs")

    val report = AgentSynthesis.synthesize(topic, facts, risks)
    println(f"[Orchestrator] Fertig nach insgesamt ${(System.nanoTime() - start) / 1e9}%.1fs")
    PipelineResult(facts, risks, report)
