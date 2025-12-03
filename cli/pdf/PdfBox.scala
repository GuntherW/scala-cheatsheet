//> using dep org.apache.pdfbox:pdfbox:3.0.6

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import scala.util.Using

@main
def pdfBox(): Unit =

  val pdfPath = "beispiel1.pdf"
  // val pdfPath = "beispiel2.pdf"

  analyzePdfReadability(pdfPath) match
    case Left(s)    => println(s"Fehler bei der Analyse: $s")
    case Right(res) => println(res)

def analyzePdfReadability(pdfPath: String): Either[String, PdfAnalysisResult] =
  Using(Loader.loadPDF(new File(pdfPath))) { document =>
    val textStripper = new PDFTextStripper()
    val text         = textStripper.getText(document)
    val trimmedText  = text.trim

    val pageCount       = document.getNumberOfPages
    val charCount       = trimmedText.length
    val wordCount       = trimmedText.split("\\s+").length
    val avgCharsPerPage = if pageCount > 0 then charCount.toDouble / pageCount else 0.0

    // Heuristik: Wenn weniger als 100 Zeichen pro Seite, wahrscheinlich gescannt
    val isMachineReadable = avgCharsPerPage >= 100

    PdfAnalysisResult(pageCount, charCount, wordCount, avgCharsPerPage, isMachineReadable)
  }.toEither.left.map(e => s"Fehler: ${e.getMessage}")

case class PdfAnalysisResult(
    pageCount: Int,
    totalChars: Int,
    totalWords: Int,
    avgCharsPerPage: Double,
    isMachineReadable: Boolean
):
  override def toString: String =
    val status = if isMachineReadable then
      "✓ Das PDF ist maschinenlesbar"
    else
      "✗ Das PDF ist wahrscheinlich nur gescannt (kein extrahierbarer Text)"
    s"""Seitenanzahl: $pageCount
       |Gesamtanzahl Zeichen: $totalChars
       |Gesamtanzahl Wörter: $totalWords
       |Durchschnittliche Zeichen pro Seite: ${f"$avgCharsPerPage%.2f"}
       |$status""".stripMargin
