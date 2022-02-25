package de.wittig.zio

import java.io.{File, FileInputStream}
import java.nio.charset.StandardCharsets

import zio.{App, Task, UIO}

object MainBracket extends App:

  // run my bracket
  def run(args: List[String]) =
    mybracket.exitCode

  def closeStream(is: FileInputStream) = UIO(is.close())

  // helper method to work around in Java 8
  def readAll(fis: FileInputStream, len: Long): Array[Byte] =
    val content: Array[Byte] = Array.ofDim(len.toInt)
    fis.read(content)
    content

  def convertBytes(is: FileInputStream, len: Long) =
    Task.attempt(println(new String(readAll(is, len), StandardCharsets.UTF_8))) // Java 8
  // Task.effect(println(new String(is.readAllBytes(), StandardCharsets.UTF_8))) // Java 11+

  // mybracket is just a value. Won't execute anything here until interpreted
  val mybracket: Task[Unit] = for
    file   <- Task(new File("input.txt"))
    len     = file.length
    string <- Task(new FileInputStream(file)).acquireReleaseWith(closeStream)(convertBytes(_, len))
  yield string
