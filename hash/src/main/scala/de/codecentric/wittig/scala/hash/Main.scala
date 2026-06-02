package de.codecentric.wittig.scala.hash

import java.security.MessageDigest
import scala.util.hashing.MurmurHash3
import com.dynatrace.hash4j.hashing.Hashing

import com.github.plokhotnyuk.jsoniter_scala.core.*

import net.jpountz.xxhash.{XXHash64, XXHashFactory}
import pt.kcry.blake3.Blake3

@main
def main(): Unit =
  val bytes = writeToArray(Person("Alice", 30, Address("Main St", "Berlin"), List("zumba", "reading", "cooking").sorted))

  println("---- nicht kryptographische Hashes ----")
  println(s"murmur3:         ${murmur3(bytes)}")
  println(s"xxhash64:        ${xxhash64(bytes)}")
  println(s"xxhash64b:       ${xxhash64b(bytes)}")
  println(s"wyhash3:         ${wyhash3(bytes)}")
  println(s"wyhash4:         ${wyhash4(bytes)}")
  println(s"komihash:        ${komihash(bytes)}")
  println(s"rapidhash:       ${rapidhash(bytes)}") // Nachfolger von Wyhash
  println("---- kryptographische Hashes ----")
  println(s"md5 (gebrochen): ${md5(bytes)}")
  println(s"blake3:          ${blake3(bytes)}")
  println(s"sha256:          ${sha256(bytes)}")

def md5(bytes: Array[Byte]): String =
  val md     = MessageDigest.getInstance("MD5")
  val digest = md.digest(bytes)
  digest.map("%02x".format(_)).mkString

def sha256(bytes: Array[Byte]): String =
  val sha    = MessageDigest.getInstance("SHA-256")
  val digest = sha.digest(bytes)
  digest.map("%02x".format(_)).mkString

def murmur3(bytes: Array[Byte]): String =
  MurmurHash3.bytesHash(bytes).toHexString

def blake3(bytes: Array[Byte]): String =
  Blake3.newHasher().update(bytes).doneHex(64)

def xxhash64(bytes: Array[Byte]): String =
  val factory          = XXHashFactory.fastestJavaInstance()
  val xxhash: XXHash64 = factory.hash64()
  xxhash.hash(bytes, 0, bytes.length, 0).toHexString

def xxhash64b(bytes: Array[Byte]): String =
  Hashing.xxh64().hashBytesToLong(bytes).toHexString

def wyhash3(bytes: Array[Byte]): String =
  Hashing.wyhashFinal3().hashBytesToLong(bytes).toHexString

def wyhash4(bytes: Array[Byte]): String =
  Hashing.wyhashFinal4().hashBytesToLong(bytes).toHexString

def komihash(bytes: Array[Byte]): String =
  Hashing.komihash5_0().hashBytesToLong(bytes).toHexString

def rapidhash(bytes: Array[Byte]): String =
  Hashing.rapidhashV3().hashBytesToLong(bytes).toHexString
