//> using dep org.apache.tika:tika-core:3.2.3
//> using dep org.apache.tika:tika-parsers-standard-package:3.2.3
//> using dep org.slf4j:slf4j-nop:2.0.17

import org.apache.tika.Tika
import org.apache.tika.metadata.Metadata
import java.io.File
import scala.util.Using
import java.io.FileInputStream

@main
def tika(): Unit =

  // val pdfPath = "beispiel1.pdf"
  val pdfPath = "beispiel2.pdf"

  checkPdfReadability(pdfPath) match
    case Left(s)           => println(s"Fehler bei der Analyse: $s")
    case Right(isReadable) =>
      if isReadable then
        println("✓ Das PDF ist maschinenlesbar")
      else
        println("✗ Das PDF ist wahrscheinlich nur gescannt (kein extrahierbarer Text)")

def checkPdfReadability(pdfPath: String): Either[String, Boolean] =
  Using(new FileInputStream(new File(pdfPath))) { stream =>
    val tika     = new Tika()
    val metadata = new Metadata()
    val text     = tika.parseToString(stream, metadata)

    println(s"Content-Type: ${metadata.get("Content-Type")}")
    println(s"Seiten: ${metadata.get("xmpTPg:NPages")}")

    text.trim.length >= 50
  }.toEither.left.map(e => s"Fehler: ${e.getMessage}")
