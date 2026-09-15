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

    // Generisch: schreibt für JEDEN im Plan tatsächlich ausgeführten Agenten eine Datei, unabhängig davon,
    // welche/wie viele Agenten der Orchestrator-Agent für dieses Thema geplant hat.
    result.outputsById.zipWithIndex.foreach { case ((agentId, output), idx) =>
      os.write.over(outputDir / f"${idx + 1}%02d_$agentId.md", s"# $agentId: $topic\n\n$output\n")
    }
    os.write.over(outputDir / "99_final_report.md", s"# Finaler Bericht: $topic\n\n${result.finalReport}\n")

    println("\n=== FINALER BERICHT ===\n")
    println(result.finalReport)
    println(s"\n[Ergebnisse gespeichert in: $outputDir]")
  finally
    AnthropicClient.close()
