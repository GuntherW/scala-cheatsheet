//> using scala 3.9.0
//> using jvm 21
//> using dep org.virtuslab::orca:0.1.10
//> using file AgentSpec.scala
//> using file ExecutionPlan.scala
//> using file PlanValidator.scala
//> using file CalculateTco.scala
//> using file AgentRegistry.scala

import orca.{*, given}

import scala.collection.immutable.ListMap

// Agentischer Orchestrator auf Orca. Das LLM plant, welcher Agent wann läuft.
// Der Flow erzwingt einen Worktree, damit der Checkout dieses Repos keinen Branch wechselt.

val defaultTopic = "Sollten wir für unser Backend von REST auf GraphQL wechseln?"
val outputDir    = "cli/multi-agent-agentic-orca/output"

case class PlanDraft(steps: List[List[String]], finalAgentId: String, reasoning: String) derives JsonData

val parsed = OrcaArgs(args)
val topic  = if parsed.userPrompt.isBlank then defaultTopic else parsed.userPrompt
val specs  = AgentRegistry.describe.map(s => s.id -> s).toMap

flow(
  parsed.copy(userPrompt = topic, target = RunTarget.Worktree),
  stackSettings = Some(StackSettings.empty),
  branchNaming = Some(
    new BranchNamingStrategy:
      def resolve(userPrompt: String, agent: Agent[?])(using InStage): String =
        BranchNamingStrategy.slug(s"agentic-report $userPrompt")
  )
):
  val draft = stage("Plan", commitMessage = Some((_: PlanDraft) => "stage: plan")):
    claude
      .withModel(AgentRegistry.model)
      .withName("Planner")
      .withReadOnly
      .withSystemPrompt(AgentRegistry.plannerPrompt)
      .resultAs[PlanDraft]
      .autonomous
      .run(s"Thema: $topic\n\nErstelle den Ausführungsplan.")

  val plan = PlanValidator.validate(ExecutionPlan(draft.steps, draft.finalAgentId, draft.reasoning), specs)
  display(s"Plan: ${plan.steps.map(_.mkString("[", ", ", "]")).mkString(" -> ")}, final=${plan.finalAgentId}")
  display(s"Begründung: ${plan.reasoning}")

  val outputs = plan.steps.zipWithIndex.foldLeft(ListMap.empty[String, String]) { case (context, (step, idx)) =>
    if step.isEmpty then context
    else
      val pairs = stage(s"Step ${idx + 1}", commitMessage = Some((_: List[(String, String)]) => s"stage: step ${idx + 1}")):
        val bound = AgentRegistry.bind(topic)
        display(s"Step ${idx + 1}: ${step.mkString(", ")}")
        Par.mapUnordered(step.size)(step): id =>
          id -> bound(id)(context)
      val byId  = pairs.toMap
      context ++ step.flatMap(id => byId.get(id).map(id -> _))
  }

  val report = stage("Berichte", commitMessage = Some((_: String) => "stage: reports")):
    plan.steps.flatten.zipWithIndex.foreach { case (id, index) =>
      val text = outputs.getOrElse(id, "")
      val name = s"${"%02d".format(index + 1)}_$id.md"
      fs.write(s"$outputDir/$name", s"# $id: $topic\n\n$text\n")
    }
    val finalReport = outputs.getOrElse(plan.finalAgentId, "Kein finaler Agent hat einen Bericht geliefert.")
    fs.write(s"$outputDir/99_final_report.md", s"# Finaler Bericht: $topic\n\n$finalReport\n")
    finalReport

  println(s"\n=== FINALER BERICHT ===\n\n$report")
  println(s"\n[Ergebnisse im Worktree: $outputDir]")
