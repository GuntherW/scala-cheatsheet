package de.codecentric.wittig.scala.compress

import java.io.*
import java.util.Base64
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import scala.util.chaining.scalaUtilChainingOps
import scala.util.Try

import de.codecentric.wittig.scala.compress.Gzip.*

@main
def main(): Unit =

  val s = "Mein schöner String, Mein schöner String, Mein schöner String, Mein schöner String, Kein schöner String"

  val gzipped = compress(s.getBytes("UTF-8")).tap(a => println(s"${new String(a)} [${a.size}]"))
  decompress(gzipped).tap(println)

  val gzippedUrlEncoded = compressAndUrlEncode(s).tap(a => println(s"${new String(a)} [${a.size}]"))
  decompressUrlDecode(gzippedUrlEncoded).tap(println)

object Gzip:

  def compress(input: Array[Byte]): Array[Byte] =
    val bos        = new ByteArrayOutputStream(input.length)
    val gzip       = new GZIPOutputStream(bos)
    gzip.write(input)
    gzip.close()
    val compressed = bos.toByteArray
    bos.close()
    compressed

  def compressAndUrlEncode(input: Array[Byte]): Array[Byte] = (Base64.getUrlEncoder.encode compose compress)(input)
  def compressAndUrlEncode(input: String): Array[Byte]      = (Base64.getUrlEncoder.encode compose compress)(input.getBytes("UTF-8"))

  def decompress(compressed: Array[Byte]): Option[String] =
    Try {
      val inputStream = new GZIPInputStream(new ByteArrayInputStream(compressed))
      scala.io.Source.fromInputStream(inputStream).mkString
    }.toOption

  def decompressUrlDecode(compressed: Array[Byte]): Option[String] =
    decompress(Base64.getUrlDecoder.decode(compressed))
