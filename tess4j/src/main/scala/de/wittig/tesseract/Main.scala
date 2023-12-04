package de.wittig.tesseract

import net.sourceforge.tess4j.Tesseract

import java.io.File

object Main extends App {

  val imageFile = new File("eurotext.tif")
  val instance  = new Tesseract // JNA Interface Mapping

  // ITesseract instance = new Tesseract1(); // JNA Direct Mapping
  instance.setDatapath("tessdata") // path to tessdata directory

  try {
    val result = instance.doOCR(imageFile)
    println(result)
  } catch {
    case e: Throwable =>
      println(e.getMessage)
  }

}
