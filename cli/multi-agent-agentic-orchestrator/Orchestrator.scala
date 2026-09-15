package agents

import ox.par

import scala.collection.immutable.ListMap

/** Der Orchestrator koordiniert den Ablauf des Multi-Agenten-Systems - jetzt in zwei sauber getrennten Phasen:
  *
  *   1. '''Planning''' (agentisch): `AgentPlanner.plan` lässt ein LLM entscheiden, welche der registrierten Agenten (`AgentRegistry.specs`) aufgerufen werden sollen, in welcher Reihenfolge und was
  *      parallel laufen kann. `PlanValidator.validate` stellt anschließend sicher, dass der Plan strukturell korrekt ist (keine verletzten Abhängigkeiten, Pflicht-Agenten enthalten, gültige
  *      `finalAgentId`), unabhängig davon, wie zuverlässig das LLM tatsächlich geantwortet hat.
  *   1. '''Execution''' (generisch, kein LLM-Call): Der validierte Plan besteht aus einer Liste von "Steps". Alle Agenten innerhalb eines Steps sind laut Plan voneinander unabhängig und werden per
  *      `ox.par` (strukturierte Nebenläufigkeit auf Virtual Threads) parallel ausgeführt (Fan-out/Fan-in pro Step); der nächste Step startet erst, wenn der aktuelle vollständig abgeschlossen ist.
  *
  * Der entscheidende Unterschied zur Vorgänger-Version: Früher stand hier hartcodiert `par(FactResearcher.research(topic), RiskAnalyst.analyze(topic))` gefolgt von `Synthesis.synthesize(...)` - ein
  * reiner, fixer "Workflow". Jetzt kennt dieser Code weder die Anzahl noch die Identität der Agenten; er führt ausschließlich aus, was `AgentRegistry` bereitstellt und `AgentPlanner` plant. Ein neuer
  * Agent lässt sich daher einbinden, indem lediglich eine neue `AgentSpec` in `AgentRegistry.specs` ergänzt wird - an diesem Executor ändert sich nichts.
  */
object Orchestrator:

  final case class Timing(planningSeconds: Double, executionSeconds: Double, totalSeconds: Double)

  /** @param outputsById
    *   Ausgaben aller ausgeführten Agenten, Reihenfolge = Ausführungsreihenfolge (`ListMap`), nicht nur Menge.
    */
  final case class PipelineResult(
      topic: String,
      plan: ExecutionPlan,
      outputsById: Map[String, String],
      finalReport: String,
      timing: Timing,
  )

  def runPipeline(topic: String): PipelineResult =
    val start = System.nanoTime()
    val specs = AgentRegistry.specs(topic).map(s => s.id -> s).toMap

    println(s"[Orchestrator] Planung: Orchestrator-Agent entscheidet über den Ausführungsplan für Thema '$topic'")
    val rawPlan         = AgentPlanner.plan(topic, specs.values.toList)
    val plan            = PlanValidator.validate(rawPlan, specs)
    val planningElapsed = (System.nanoTime() - start) / 1e9
    println(f"[Orchestrator] Plan (nach Validierung, ${plan.steps.size} Step(s)): ${plan.steps.map(_.mkString("[", ", ", "]")).mkString(" -> ")}, final=${plan.finalAgentId}")
    println(s"[Orchestrator] Begründung des Orchestrator-Agent: ${plan.reasoning}")

    val executionStart   = System.nanoTime()
    val totalSteps       = plan.steps.size
    val outputs          = plan.steps.zipWithIndex.foldLeft(ListMap.empty[String, String]) { case (contextSoFar, (step, idx)) =>
      val stepNo      = idx + 1
      val parallel    = step.size > 1
      val stepLabel   = if parallel then "PARALLEL" else "SEQUENTIELL"
      println(s"[Orchestrator] Step $stepNo/$totalSteps START ($stepLabel): ${step.mkString(", ")}")
      val stepStart   = System.nanoTime()
      val results     = par(step.map(id => () => specs(id).execute(contextSoFar)))
      val stepElapsed = (System.nanoTime() - stepStart) / 1e9
      println(f"[Orchestrator] Step $stepNo/$totalSteps ENDE ($stepLabel) nach $stepElapsed%.1fs: ${step.mkString(", ")}")
      contextSoFar ++ step.zip(results)
    }
    val executionElapsed = (System.nanoTime() - executionStart) / 1e9

    val totalElapsed = (System.nanoTime() - start) / 1e9
    println(f"[Orchestrator] Fertig nach insgesamt $totalElapsed%.1fs (Planning: $planningElapsed%.1fs, Execution: $executionElapsed%.1fs)")

    val finalReport = outputs.getOrElse(
      plan.finalAgentId,
      throw new IllegalStateException(s"finalAgentId '${plan.finalAgentId}' hat keinen Output erzeugt - Plan/Executor inkonsistent."),
    )

    PipelineResult(
      topic = topic,
      plan = plan,
      outputsById = outputs,
      finalReport = finalReport,
      timing = Timing(planningElapsed, executionElapsed, totalElapsed),
    )
