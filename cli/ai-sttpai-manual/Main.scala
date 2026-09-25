package agents

/** Einstiegspunkt: Führt das Multi-Agenten-System für ein Beispielthema aus und schreibt die Zwischenergebnisse sowie den finalen Bericht in Textdateien im Ordner `./output` (Dateizugriff über
  * os-lib).
  *
  * Aufruf:
  * {{{
  *   scala-cli run . -- "Sollten wir Kubernetes für unser 5-Personen-Startup einführen?"
  * }}}
  *
  * Ohne Argument wird ein Standard-Thema verwendet.
  */
@main def main(args: String*): Unit =
  val defaultTopic = "Sollten wir für unser Backend von REST auf GraphQL wechseln?"
  val topic        = args.headOption.getOrElse(defaultTopic)

  try
    val result = Orchestrator.runPipeline(topic)

    val outputDir = os.pwd / "output"
    os.makeDir.all(outputDir)

    os.write.over(outputDir / "01_fact_researcher.md", s"# Fact-Researcher: $topic\n\n${result.factResearcherOutput}\n")
    os.write.over(outputDir / "02_risk_analyst.md", s"# Risk-Analyst: $topic\n\n${result.riskAnalystOutput}\n")
    os.write.over(outputDir / "03_final_report.md", s"# Finaler Bericht: $topic\n\n${result.finalReport}\n")

    println("\n=== FINALER BERICHT ===\n")
    println(result.finalReport)
    println(s"\n[Ergebnisse gespeichert in: $outputDir]")
  finally
    AnthropicClient.close()
