package rag

/** Einstiegspunkt mit drei Subcommands:
  *
  * {{{
  *   scala-cli run . -- generate-docs                  # erzeugt die Beispiel-PDFs in docs/ (einmalig)
  *   scala-cli run . -- ingest                         # liest alle PDFs im docs-Ordner ein, befüllt pgvector neu
  *   scala-cli run . -- ask "Welche Frage auch immer?"  # beantwortet eine Frage via RAG-Pipeline
  * }}}
  *
  * Ohne Argument wird `ask` mit einer Standardfrage zum Beispiel-Korpus (siehe docs-Ordner) verwendet.
  */
@main def main(args: String*): Unit =
  val docsDir = os.pwd / "docs"

  args.toList match
    case "generate-docs" :: _ =>
      GenerateSampleDocs.run(docsDir)

    case "ingest" :: _ =>
      Ingestion.run(docsDir)

    case "ask" :: question :: _ =>
      askAndPrint(question)

    case Nil =>
      askAndPrint("Was ist Retrieval-Augmented Generation und wofür braucht man Chunking?")

    case other =>
      println(s"Unbekannte Argumente: ${other.mkString(" ")}")
      println("""Nutzung:
          |  scala-cli run . -- generate-docs
          |  scala-cli run . -- ingest
          |  scala-cli run . -- ask "<Frage>"""".stripMargin)

private def askAndPrint(question: String): Unit =
  try
    val result = RagPipeline.answer(question)

    val outputDir = os.pwd / "output"
    os.makeDir.all(outputDir)
    os.write.over(
      outputDir / "answer.md",
      s"""# Frage: ${result.question}
         |
         |**Umformulierte Suchanfrage:** ${result.rewrittenQuery}
         |
         |## Verwendete Quellen
         |${result.usedChunks.map(h => s"- ${h.sourceFile}#${h.chunkIndex} (distance=${h.distance})").mkString("\n")}
         |
         |## Antwort
         |
         |${result.answer}
         |""".stripMargin,
    )

    println("\n=== ANTWORT ===\n")
    println(result.answer)
    println(s"\n[Ergebnis gespeichert in: $outputDir/answer.md]")
  finally AnthropicClient.close()
