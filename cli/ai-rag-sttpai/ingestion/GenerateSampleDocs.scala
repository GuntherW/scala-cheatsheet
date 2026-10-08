package rag.ingestion

import org.apache.pdfbox.pdmodel.{PDDocument, PDPage, PDPageContentStream}
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.{PDType1Font, Standard14Fonts}

/** Erzeugt die Beispiel-PDFs in `docs/` (einmalig auszuführen, siehe `Main.scala`, Subcommand `generate-docs`).
  *
  * Die Inhalte sind bewusst frei erfunden/vereinfacht und enthalten ein paar konkrete, eindeutig abfragbare Fakten (Zahlen, Namen) - so lässt sich beim Testen der Pipeline leicht prüfen, ob die
  * Antwort tatsächlich aus dem jeweils richtigen Dokument stammt (Grounding) statt aus dem allgemeinen Trainingswissen des Modells.
  */
object GenerateSampleDocs:

  def run(docsDir: os.Path): Unit =
    os.makeDir.all(docsDir)
    SampleDocs.all.foreach { case (fileName, paragraphs) =>
      writePdf(docsDir / fileName, paragraphs)
      println(s"[GenerateSampleDocs] geschrieben: ${docsDir / fileName}")
    }

private def writePdf(path: os.Path, paragraphs: List[String]): Unit =
  val doc  = new PDDocument()
  val font = new PDType1Font(Standard14Fonts.FontName.HELVETICA)

  val margin       = 50f
  val fontSize     = 11f
  val leading      = 1.4f * fontSize
  val maxLineChars = 95

  def wrapLines(text: String): List[String] =
    text
      .split("\n")
      .toList
      .flatMap { line =>
        if line.isEmpty then List("")
        else
          val words = line.split(" ").toList
          words
            .foldLeft(List("")) { (acc, word) =>
              val current = acc.head
              val tryLine = if current.isEmpty then word else s"$current $word"
              if tryLine.length <= maxLineChars then tryLine :: acc.tail else word :: acc
            }
            .reverse
      }

  var page   = new PDPage(PDRectangle.A4)
  doc.addPage(page)
  var stream = new PDPageContentStream(doc, page)
  var y      = page.getMediaBox.getHeight - margin
  stream.setFont(font, fontSize)
  stream.beginText()
  stream.newLineAtOffset(margin, y)

  def newPage(): Unit =
    stream.endText()
    stream.close()
    page = new PDPage(PDRectangle.A4)
    doc.addPage(page)
    stream = new PDPageContentStream(doc, page)
    y = page.getMediaBox.getHeight - margin
    stream.setFont(font, fontSize)
    stream.beginText()
    stream.newLineAtOffset(margin, y)

  for paragraph <- paragraphs do
    for line <- wrapLines(paragraph) do
      if y <= margin then newPage()
      stream.showText(line)
      stream.newLineAtOffset(0, -leading)
      y -= leading
    // Leerzeile zwischen Absätzen
    if y <= margin then newPage()
    stream.newLineAtOffset(0, -leading)
    y -= leading

  stream.endText()
  stream.close()
  doc.save(path.toString)
  doc.close()
