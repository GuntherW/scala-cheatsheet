package agents

import kyo.*

import scala.collection.immutable.ListMap

/** Der Orchestrator koordiniert den Ablauf des Multi-Agenten-Systems - zwei sauber getrennte Phasen:
  *
  *   1. '''Planning''' (agentisch): `AgentPlanner.plan` lässt ein LLM entscheiden, welche der registrierten Agenten (`AgentRegistry.specs`) aufgerufen werden sollen, in welcher Reihenfolge und was
  *      parallel laufen kann. `PlanValidator.validate` stellt anschließend sicher, dass der Plan strukturell korrekt ist.
  *   1. '''Execution''' (generisch, kein LLM-Call in diesem Code selbst): Der validierte Plan besteht aus einer Liste von "Steps". Alle Agenten innerhalb eines Steps sind laut Plan voneinander
  *      unabhängig und werden per `Async.foreach` (kyo-eigene strukturierte Nebenläufigkeit, Ersatz für `ox.par` im sttp-ai-Original) parallel ausgeführt; der nächste Step startet erst, wenn der
  *      aktuelle vollständig abgeschlossen ist.
  *
  * Dieser Code kennt weder die Anzahl noch die Identität der Agenten; er führt ausschließlich aus, was `AgentRegistry` bereitstellt und `AgentPlanner` plant. Ein neuer Agent lässt sich daher
  * einbinden, indem lediglich eine neue `AgentSpec` in `AgentRegistry.specs` ergänzt wird.
  */
object Orchestrator:

  final case class Timing(planningSeconds: Double, executionSeconds: Double, totalSeconds: Double)

  /** @param outputsById
    *   Ausgaben aller ausgeführten Agenten, Reihenfolge = Ausführungsreihenfolge (`ListMap`), nicht nur Menge.
    * @param usageReport
    *   Menschenlesbare Zusammenfassung über Tokens/Kosten/Dauer aller LLM-Calls dieser Pipeline (Planner + alle ausgeführten Worker-Agenten) - siehe `Observability.UsageCollector.report`.
    */
  final case class PipelineResult(
      topic: String,
      plan: ExecutionPlan,
      outputsById: ListMap[String, String],
      finalReport: String,
      timing: Timing,
      usageReport: String,
  )

  def runPipeline(topic: String): PipelineResult < (Async & Sync) =
    for
      start            <- Sync.defer(java.lang.System.nanoTime())
      specsList         = AgentRegistry.specs(topic)
      specs             = specsList.map(s => s.id -> s).toMap
      _                <- Log.info(s"[Orchestrator] Planung: Orchestrator-Agent entscheidet über den Ausführungsplan für Thema '$topic'")
      rawPlan          <- AgentPlanner.plan(topic, specsList)
      plan              = PlanValidator.validate(rawPlan, specs)
      planningElapsed  <- Sync.defer((java.lang.System.nanoTime() - start) / 1e9)
      _                <- Log.info(f"[Orchestrator] Plan (nach Validierung, ${plan.steps.size} Step(s)): ${plan.steps.map(_.mkString("[", ", ", "]")).mkString(" -> ")}, final=${plan.finalAgentId}")
      _                <- Log.info(s"[Orchestrator] Begründung des Orchestrator-Agent: ${plan.reasoning}")
      executionStart   <- Sync.defer(java.lang.System.nanoTime())
      outputs          <- runSteps(plan.steps, specs)
      executionElapsed <- Sync.defer((java.lang.System.nanoTime() - executionStart) / 1e9)
      totalElapsed     <- Sync.defer((java.lang.System.nanoTime() - start) / 1e9)
      _                <- Log.info(f"[Orchestrator] Fertig nach insgesamt $totalElapsed%.1fs (Planning: $planningElapsed%.1fs, Execution: $executionElapsed%.1fs)")
      finalReport       = outputs.getOrElse(
                            plan.finalAgentId,
                            throw new IllegalStateException(s"finalAgentId '${plan.finalAgentId}' hat keinen Output erzeugt - Plan/Executor inkonsistent."),
                          )
      usageReport       = Observability.usageCollector.report(Observability.Pricing.table)
      _                <- Log.info(usageReport)
    yield PipelineResult(
      topic = topic,
      plan = plan,
      outputsById = outputs,
      finalReport = finalReport,
      timing = Timing(planningElapsed, executionElapsed, totalElapsed),
      usageReport = usageReport,
    )

  /** Führt die Steps eines validierten Plans sequenziell aus (Fan-out/Fan-in PRO Step über `Async.foreach`) und faltet die Ergebnisse in eine `ListMap` (Reihenfolge = Ausführungsreihenfolge) zusammen -
    * jeder folgende Step bekommt bereits alle bisherigen Outputs als Kontext.
    */
  private def runSteps(steps: List[List[String]], specs: Map[String, AgentSpec]): ListMap[String, String] < (Async & Sync) =
    val totalSteps = steps.size

    def loop(remaining: List[(List[String], Int)], acc: ListMap[String, String]): ListMap[String, String] < (Async & Sync) =
      remaining match
        case Nil                 => acc
        case (step, idx) :: rest =>
          val stepNo    = idx + 1
          val parallel  = step.size > 1
          val stepLabel = if parallel then "PARALLEL" else "SEQUENTIELL"
          for
            _           <- Log.info(s"[Orchestrator] Step $stepNo/$totalSteps START ($stepLabel): ${step.mkString(", ")}")
            stepStart   <- Sync.defer(java.lang.System.nanoTime())
            results     <- Async.foreach(step, concurrency = step.size)(id => specs(id).execute(acc))
            stepElapsed <- Sync.defer((java.lang.System.nanoTime() - stepStart) / 1e9)
            _           <- Log.info(f"[Orchestrator] Step $stepNo/$totalSteps ENDE ($stepLabel) nach $stepElapsed%.1fs: ${step.mkString(", ")}")
            next         = acc ++ step.zip(results.toList)
            result      <- loop(rest, next)
          yield result

    loop(steps.zipWithIndex, ListMap.empty)
