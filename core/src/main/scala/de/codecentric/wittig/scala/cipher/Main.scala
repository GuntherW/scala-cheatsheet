package de.codecentric.wittig.scala.cipher

import java.security.Security
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}

object Main extends App {

  private val passphrase = "LwCeAPO6amEeLGNMK9yb895QGKVhalhyrWO+6bjNIJM="
  private val key        = new SecretKeySpec(Base64.getDecoder.decode(passphrase), "AES")

  Security.getProviders().foreach(println)

  aes()
  aesCbc()

  private def aes() = {
    val cipher = Cipher.getInstance("AES")

    cipher.init(Cipher.ENCRYPT_MODE, key)
    val encrypted = cipher.doFinal("OriginalNachricht".getBytes("UTF-8"))

    cipher.init(Cipher.DECRYPT_MODE, key)
    val decrypted = cipher.doFinal(encrypted)
    println(new String(decrypted))
  }

  private def aesCbc() = {
    val cipher    = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val BlockSize = 16

    cipher.init(Cipher.ENCRYPT_MODE, key)
    val encrypted   = cipher.doFinal("OriginalNachricht".getBytes("UTF-8"))
    val iv          = cipher.getIV
    val encryptedIv = iv ++ encrypted

    cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(encryptedIv, 0, BlockSize))
    val decrypted = cipher.doFinal(encryptedIv, BlockSize, encryptedIv.length - BlockSize)
    println(new String(decrypted))
  }

}
