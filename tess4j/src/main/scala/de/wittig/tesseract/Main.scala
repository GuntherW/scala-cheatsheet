package de.wittig.tesseract

import net.sourceforge.tess4j.{Tesseract, Tesseract1}
import java.io.File

object Main extends App {

  val imageFile = new File("tess4j/src/main/resources/img.png")
//  val imageFile = new File("tess4j/src/main/resources/img_1.png")
  val instance  = new Tesseract // JNA Interface Mapping

//  val instance = new Tesseract1(); // JNA Direct Mapping
  instance.setDatapath("tess4j/src/main/resources/tessdata")
  instance.setLanguage("deu")
//  instance.setPageSegMode(1)
  instance.setOcrEngineMode(1)

  try {
    val result = instance.doOCR(imageFile)
    println(result)
  } catch {
    case e: Throwable =>
      println(e.getMessage)
  }

}
