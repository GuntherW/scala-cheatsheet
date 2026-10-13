package rag

import org.llm4s.chunking.{ChunkerFactory, ChunkingConfig}

/** Testet nur den reinen Chunking-Schritt (`ChunkerFactory.sentence`, auch von `RagFactory` genutzt) - ohne LLM-/DB-Call. */
class ChunkingTest extends munit.FunSuite:

  private val config = ChunkingConfig(targetSize = 200, maxSize = 300, overlap = 20)

  test("sentence chunker zerlegt einen langen Text in mehrere, nicht zu lange Chunks") {
    val longText = (1 to 50).map(i => s"Satz Nummer $i enthält ein paar Wörter zum Testen des Chunkers.").mkString(" ")
    val chunks   = ChunkerFactory.sentence().chunk(longText, config)

    assert(chunks.size > 1, "ein so langer Text sollte in mehrere Chunks zerlegt werden")
    assert(chunks.forall(_.content.length <= 300 + config.overlap), "Chunks dürfen maxSize (plus Overlap) nicht deutlich überschreiten")
  }

  test("ein kurzer Text bleibt ein einzelner Chunk") {
    val chunks = ChunkerFactory.sentence().chunk("Ein kurzer Beispieltext.", config)

    assertEquals(chunks.size, 1)
    assertEquals(chunks.head.content, "Ein kurzer Beispieltext.")
  }
