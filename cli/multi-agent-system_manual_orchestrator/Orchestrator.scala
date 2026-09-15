package agents

import ox.par

/** Der Orchestrator steuert den Ablauf des Multi-Agenten-Systems:
  *
  *   1. Fact-Researcher und Risk-Analyst werden PARALLEL gestartet (`ox.par`, strukturierte Nebenläufigkeit auf Basis von Virtual Threads), da sie unabhängig voneinander arbeiten und keine
  *      gemeinsamen Zwischenergebnisse benötigen. Das spart Laufzeit (Latenz beider Model-Calls überlappt sich, statt sich zu addieren).
  *   1. Sobald BEIDE Worker fertig sind, erhält der Synthesis-Agent beide Ergebnisse als Kontext und erstellt den finalen Bericht.
  *
  * Dieses Muster nennt man "Fan-out / Fan-in":
  *   - '''Fan-out''': Eine Aufgabe wird an mehrere unabhängige Worker verteilt.
  *   - '''Fan-in''': Die Ergebnisse aller Worker werden an einer Stelle wieder zusammengeführt (hier: durch den Synthesis-Agent).
  *
  * `ox.par` kapselt dabei bereits das komplette Fan-out/Fan-in: Beide Berechnungen werden auf eigenen (virtuellen) Threads gestartet, `par` kehrt erst zurück, wenn BEIDE fertig sind (strukturierte
  * Nebenläufigkeit - im Gegensatz zu freischwebenden `Future`s ist hier durch den Scope garantiert, dass keine "verwaisten" Hintergrund-Threads übrig bleiben). Schlägt eine der beiden Berechnungen
  * fehl, wird die andere automatisch abgebrochen (interrupted) und der Fehler propagiert.
  */
object Orchestrator:

  final case class Timing(workersSeconds: Double, totalSeconds: Double)

  final case class PipelineResult(
      topic: String,
      factResearcherOutput: String,
      riskAnalystOutput: String,
      finalReport: String,
      timing: Timing,
  )

  def runPipeline(topic: String): PipelineResult =
    println(s"[Orchestrator] Starte Fact-Researcher und Risk-Analyst PARALLEL für: '$topic'")
    val start = System.nanoTime()

    // Fan-out + Fan-in in einem Aufruf: `par` startet beide Berechnungen
    // parallel und liefert erst zurück, wenn beide abgeschlossen sind.
    val (facts, risks) = par(
      AgentFactResearcher.research(topic),
      RiskAnalyst.analyze(topic),
    )

    val workersElapsed = (System.nanoTime() - start) / 1e9
    println(f"[Orchestrator] Beide Worker fertig nach $workersElapsed%.1fs")

    println("[Orchestrator] Starte Synthesis-Agent (sequentiell, benötigt beide Vorergebnisse)")
    val report = AgentSynthesis.synthesize(topic, facts, risks)

    val totalElapsed = (System.nanoTime() - start) / 1e9
    println(f"[Orchestrator] Fertig nach insgesamt $totalElapsed%.1fs")

    PipelineResult(
      topic = topic,
      factResearcherOutput = facts,
      riskAnalystOutput = risks,
      finalReport = report,
      timing = Timing(workersElapsed, totalElapsed),
    )
