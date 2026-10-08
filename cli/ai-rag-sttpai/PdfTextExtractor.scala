package rag

import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper

import java.io.File

/** Extrahiert reinen Text aus PDF-Dateien via Apache PDFBox - der erste Schritt der Ingestion-Pipeline (siehe `Ingestion.scala`). */
object PdfTextExtractor:

  def extractText(pdfFile: os.Path): String =
    val file = new File(pdfFile.toString)
    val doc  = Loader.loadPDF(file)
    try new PDFTextStripper().getText(doc)
    finally doc.close()
