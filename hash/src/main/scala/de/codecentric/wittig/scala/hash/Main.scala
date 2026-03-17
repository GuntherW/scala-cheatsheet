package de.codecentric.wittig.scala.hash

import java.security.MessageDigest
import scala.util.hashing.MurmurHash3

import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*

import net.jpountz.xxhash.{XXHash64, XXHashFactory}
import pt.kcry.blake3.Blake3

@main
def main(): Unit =
  val personUnsorted = Person("Alice", 30, Address("Main St", "Berlin"), List("zumba", "reading", "cooking"))
  val jsonUnsorted   = personUnsorted.asJson.noSpaces

  val personSorted = Person("Alice", 30, Address("Main St", "Berlin"), List("cooking", "reading", "zumba"))
  val jsonSorted   = personSorted.asJson.noSpaces

  println("=== Unsortierte Liste (beim Erstellen) ===")
  println(s"Person: $personUnsorted")
  println(s"JSON:   $jsonUnsorted")
  println(s"Hash:   ${hashSha256(jsonUnsorted.getBytes)}")
  println()

  println("=== Sortierte Liste (beim Erstellen) ===")
  println(s"Person: $personSorted")
  println(s"JSON:   $jsonSorted")
  println(s"Hash:   ${hashSha256(jsonSorted.getBytes)}")
  println()

  println("=== JSON mit unsortierter Liste wird beim Parsen sortiert ===")
  val parsed = decode[Person](jsonUnsorted).getOrElse(throw new Exception("parse error"))
  println(s"Parsed: $parsed")
  println(s"Hash:   ${hashSha256(parsed.asJson.noSpaces.getBytes)}")
  println()

  val bytes = jsonUnsorted.getBytes
  println(s"md5:          ${hashMd5(bytes)}")
  println(s"hashSha256:   ${hashSha256(bytes)}")
  println(s"hashMurmur3:  ${hashMurmur3(bytes)}")
  println(s"hashBlake3:   ${hashBlake3(bytes)}")
  println(s"hashXxhash64: ${hashXxhash64(bytes)}")

def hashMd5(bytes: Array[Byte]): String =
  val md     = MessageDigest.getInstance("MD5")
  val digest = md.digest(bytes)
  digest.map("%02x".format(_)).mkString

def hashSha256(bytes: Array[Byte]): String =
  val sha    = MessageDigest.getInstance("SHA-256")
  val digest = sha.digest(bytes)
  digest.map("%02x".format(_)).mkString

def hashMurmur3(bytes: Array[Byte]): String =
  val hash = MurmurHash3.bytesHash(bytes)
  Integer.toHexString(hash)

def hashBlake3(bytes: Array[Byte]): String =
  Blake3.newHasher().update(bytes).doneHex(64)

def hashXxhash64(bytes: Array[Byte]): String =
  val factory          = XXHashFactory.fastestJavaInstance()
  val xxhash: XXHash64 = factory.hash64()
  val hash             = xxhash.hash(bytes, 0, bytes.length, 0)
  java.lang.Long.toHexString(hash)
