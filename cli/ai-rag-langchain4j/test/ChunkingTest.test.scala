package rag

import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.splitter.DocumentSplitters

import scala.jdk.CollectionConverters.*

/** Testet nur den reinen Chunking-Schritt (`DocumentSplitters.recursive`, auch von `ingestion/Ingestion.scala` genutzt) - ganz ohne LLM-/DB-Call, damit dieser Test ohne laufenden `postgres-rag`-
  * Container und ohne gültigen Requesty-Key funktioniert (`scala-cli test .`).
  */
class ChunkingTest extends munit.FunSuite:

  test("recursive splitter zerlegt einen langen Text in mehrere, nicht zu lange Chunks") {
    val longParagraph = (1 to 50).map(i => s"Satz Nummer $i enthält ein paar Wörter zum Testen des Splitters.").mkString(" ")
    val document      = Document.from(longParagraph)

    val splitter = DocumentSplitters.recursive(200, 20)
    val chunks   = splitter.split(document).asScala.toList

    assert(chunks.size > 1, "ein so langer Text sollte in mehrere Chunks zerlegt werden")
    assert(chunks.forall(_.text().length <= 250), "Chunks sollten die konfigurierte Maximalgröße (inkl. etwas Toleranz) nicht deutlich überschreiten")
    assertEquals(chunks.map(_.text()).mkString(" ").replace("  ", " ").contains("Satz Nummer 1 "), true)
  }

  test("ein kurzer Text bleibt ein einzelner Chunk") {
    val document = Document.from("Ein kurzer Beispieltext.")
    val splitter = DocumentSplitters.recursive(500, 50)
    val chunks   = splitter.split(document).asScala.toList

    assertEquals(chunks.size, 1)
    assertEquals(chunks.head.text(), "Ein kurzer Beispieltext.")
  }
