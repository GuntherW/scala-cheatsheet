package de.codecentric.wittig.scala.capturechecking.docu

import java.io.FileOutputStream
import language.experimental.captureChecking

@main
def main(): Unit = {

  // OK
  usingLogFile(logFile => logFile.write(42))

  val later = usingLogFile(logFile => () => logFile.write(42))
  later() // IOException: Stream closed
}

def usingLogFile[T](op: FileOutputStream => T): T =
  val logFile = new FileOutputStream("log.txt")
  val result  = op(logFile)
  logFile.close()
  result

def usingLogFileCC[T](op: FileOutputStream^ => T): T =
  val logFile = new FileOutputStream("log.txt")
  val result  = op(logFile)
  logFile.close()
  result
