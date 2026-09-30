package agent

/** Einstiegspunkt: Führt den Single-Agenten für eine Beispielfrage aus, die bewusst mehrere Tools nacheinander erfordert (Uhrzeit + Würfeln), und schreibt das Ergebnis nach `./output`.
  *
  * Aufruf:
  * {{{
  *   scala-cli run . -- "Wie spät ist es gerade, und würfle danach zweimal einen 20-seitigen Würfel?"
  * }}}
  *
  * Ohne Argument wird eine Standardfrage verwendet. Die komplette Konsolenausgabe (siehe `AnthropicClient.log`) zeigt den kompletten Tool-Search-Flow: `search_tools`-Aufrufe, Treffer, Freischaltung
  * der gefundenen Tools und die eigentlichen Tool-Aufrufe.
  */
@main def main(args: String*): Unit =
  val defaultQuestion = "Wie spät ist es gerade in Europe/Berlin, und würfle danach zweimal einen 20-seitigen Würfel und addiere das Ergebnis?"
  val question        = args.headOption.getOrElse(defaultQuestion)

  try
    println(s"[Main] Frage: $question\n")
    val answer = Agent.run(question)

    val outputDir = os.pwd / "output"
    os.makeDir.all(outputDir)
    os.write.over(outputDir / "answer.md", s"# Frage\n\n$question\n\n# Antwort\n\n$answer\n")

    println("\n=== ANTWORT ===\n")
    println(answer)
    println(s"\n[Ergebnis gespeichert in: $outputDir]")
  finally AnthropicClient.close()
