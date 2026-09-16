package de.wittig.sttp

import java.nio.file.Files
import java.nio.file.Path

object TempFiles:
  def withTemporaryFile[T](data: Array[Byte])(f: Path => T): T =
    val file = Files.createTempFile("sttp", "demo")
    try
      Files.write(file, data)
      f(file)
    finally
      Files.deleteIfExists(file): Unit
