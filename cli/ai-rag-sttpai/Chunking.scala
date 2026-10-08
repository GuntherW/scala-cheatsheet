package rag

/** Teilt einen langen Text in überlappende Chunks - reine, LLM-freie Logik (daher gut testbar, siehe `ChunkingTest.test.scala`).
  *
  * '''Warum Chunking überhaupt?''' Ein Embedding-Vektor (hier via `HashingTfIdfEmbedder`) repräsentiert ein ganzes Textstück als EINEN Vektor - je länger und thematisch breiter der Text, desto
  * "verwaschener"/unspezifischer wird dieser Vektor (viele unterschiedliche Themen mitteln sich gegenseitig weg). Kleinere, in sich abgeschlossene Chunks liefern präzisere Treffer bei der
  * Ähnlichkeitssuche und passen außerdem besser in das Kontextfenster des Synthesis-Agent.
  *
  * '''Warum Overlap?''' Ohne Überlappung könnte ein für die Antwort entscheidender Satz genau an einer Chunk-Grenze zerschnitten werden und dadurch in keinem der beiden Chunks mehr vollständig (und
  * damit nicht mehr zuverlässig als relevant erkennbar) vorkommen. Der Overlap dupliziert bewusst einen kleinen Teil des Textes in den jeweils benachbarten Chunk.
  */
object Chunking:

  /** @param maxChars
    *   maximale Chunk-Länge in Zeichen.
    * @param overlapChars
    *   Anzahl der Zeichen, die sich zwei aufeinanderfolgende Chunks überlappen.
    */
  def chunk(text: String, maxChars: Int = 800, overlapChars: Int = 150): List[String] =
    require(overlapChars < maxChars, "overlapChars muss kleiner als maxChars sein, sonst kommt die Fensterung nie voran.")

    val normalized = text.replaceAll("\\s+", " ").trim
    if normalized.isEmpty then Nil
    else
      val step = maxChars - overlapChars
      Iterator
        .iterate(0)(_ + step)
        .takeWhile(_ < normalized.length)
        .map(start => normalized.substring(start, math.min(start + maxChars, normalized.length)))
        .filter(_.nonEmpty)
        .toList
