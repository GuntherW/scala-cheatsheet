package rag

import rag.ingestion.Chunking

import munit.FunSuite

class ChunkingTest extends FunSuite:

  test("leerer Text ergibt keine Chunks"):
    assertEquals(Chunking.chunk(""), Nil)
    assertEquals(Chunking.chunk("   \n  "), Nil)

  test("kurzer Text (unter maxChars) ergibt genau einen Chunk"):
    val text   = "Dies ist ein kurzer Beispieltext."
    val chunks = Chunking.chunk(text, maxChars = 800, overlapChars = 150)
    assertEquals(chunks, List(text))

  test("langer Text wird in mehrere, überlappende Chunks aufgeteilt"):
    val text   = ("Wort " * 500).trim // deutlich länger als maxChars
    val chunks = Chunking.chunk(text, maxChars = 100, overlapChars = 20)
    assert(chunks.size > 1, s"Erwartet mehrere Chunks, war aber: ${chunks.size}")
    chunks.foreach(c => assert(c.length <= 100, s"Chunk zu lang: ${c.length}"))

  test("Overlap: Ende eines Chunks taucht am Anfang des nächsten wieder auf"):
    val text          = (1 to 300).map(i => s"wort$i").mkString(" ")
    val chunks        = Chunking.chunk(text, maxChars = 100, overlapChars = 30)
    assert(chunks.size >= 2)
    val endOfFirst    = chunks(0).takeRight(30)
    val startOfSecond = chunks(1).take(30)
    assertEquals(endOfFirst, startOfSecond)

  test("require schlägt fehl, wenn overlapChars >= maxChars"):
    intercept[IllegalArgumentException]:
      Chunking.chunk("ein Text", maxChars = 50, overlapChars = 50)
