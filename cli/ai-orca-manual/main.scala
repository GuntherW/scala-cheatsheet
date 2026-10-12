//> using scala 3.9.0
//> using dep org.virtuslab::orca:0.2.0
//> using file CalculateTco.scala
//> using file Agents.scala

import orca.{*, given}

// Der Flow erzwingt einen Worktree, damit der Checkout dieses Repos keinen Branch wechselt.
// Reports landen im Worktree unter cli/ai-orca-manual/output/.

val defaultTopic = "Sollten wir für unser Backend von REST auf GraphQL wechseln?"
val outputDir = "cli/ai-orca-manual/output"

@main def main(args: String*): Unit =
  val parsed = OrcaArgs(args.toArray)
  val topic  = if parsed.userPrompt.isBlank then defaultTopic else parsed.userPrompt

  flow(
    parsed.copy(userPrompt = topic, target = RunTarget.Worktree),
    stackSettings = Some(StackSettings.empty),
    branchNaming = Some(
      new BranchNamingStrategy:
        def resolve(userPrompt: String, agent: Agent[?])(using InStage): String = BranchNamingStrategy.slug(s"decision-report $userPrompt")
    ),
  ) {
    val reports = stage("Workers", commitMessage = Some((_: WorkerReports) => "stage: worker reports")):
      val tco = CalculateTco.estimate(topic)
      display(s"Starte Fact-Researcher und Risk-Analyst parallel für: '$topic'")
      val tagged = Par.mapUnordered(2)(List(Worker.Fact, Worker.Risk)) {
        case Worker.Fact => Worker.Fact -> Agents.factResearcher(topic)
        case Worker.Risk => Worker.Risk -> Agents.riskAnalyst(topic, tco)
      }
      val byRole = tagged.toMap
      WorkerReports(fact = byRole(Worker.Fact), risk = byRole(Worker.Risk))

    val report = stage("Synthesis", commitMessage = Some((_: String) => "stage: final report")):
      display("Starte Synthesis-Agent")
      val text = Agents.synthesis(topic, reports)
      fs.write(s"$outputDir/01_fact_researcher.md", s"# Fact-Researcher: $topic\n\n${reports.fact}\n")
      fs.write(s"$outputDir/02_risk_analyst.md", s"# Risk-Analyst: $topic\n\n${reports.risk}\n")
      fs.write(s"$outputDir/03_final_report.md", s"# Finaler Bericht: $topic\n\n$text\n")
      text

    println(s"\n=== FINALER BERICHT ===\n\n$report")
    println(s"\n[Ergebnisse im Worktree: $outputDir]")
  }