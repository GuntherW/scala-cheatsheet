package agents

import java.nio.file.{Files, Path}

/** Aufruf: `scala-cli run . -- "Sollten wir Kubernetes für unser 5-Personen-Startup einführen?"` - ohne Argument wird ein Standardthema verwendet. */
@main def main(args: String*): Unit =
  val topic  = args.headOption.getOrElse("Sollten wir für unser Backend von REST auf GraphQL wechseln?")
  val result = Orchestrator.runPipeline(topic)

  val outputDir = Files.createDirectories(Path.of("output"))
  Seq(
    "01_fact_researcher.md" -> s"# Fact-Researcher: $topic\n\n${result.facts}\n",
    "02_risk_analyst.md"    -> s"# Risk-Analyst: $topic\n\n${result.risks}\n",
    "03_final_report.md"    -> s"# Finaler Bericht: $topic\n\n${result.report}\n",
  ).foreach((file, text) => Files.writeString(outputDir.resolve(file), text))

  println(s"\n=== FINALER BERICHT ===\n\n${result.report}\n\n[Ergebnisse gespeichert in: ${outputDir.toAbsolutePath}]")
