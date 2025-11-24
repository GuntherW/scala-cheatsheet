package de.wittig.json.jwt
import org.bouncycastle.jce.provider.BouncyCastleProvider
import pdi.jwt.{Jwt, JwtAlgorithm}

import java.security.spec.ECGenParameterSpec
import java.security.{KeyPairGenerator, SecureRandom, Security}
import scala.util.chaining.scalaUtilChainingOps

@main
def mainWithGeneratedKeysEcdsa(): Unit =

  val ecGenSpec = ECGenParameterSpec("P-521") // We specify the curve we want to use

  // We are going to use a ECDSA algorithm and the Bouncy Castle provider
  if (Security.getProvider("BC") == null) {
    Security.addProvider(BouncyCastleProvider())
  }
  val generatorEC = KeyPairGenerator.getInstance("ECDSA", "BC")
  generatorEC.initialize(ecGenSpec, SecureRandom())

  // Generate a pair of keys, one private for encoding and one public for decoding
  val ecKey = generatorEC.generateKeyPair()
  ecKey.getPrivate.tap(println)
  ecKey.getPublic.tap(println)

  val token   = Jwt.encode("""{"user":1}""", ecKey.getPrivate, JwtAlgorithm.ES512)
  val decoded = Jwt.decode(token, ecKey.getPublic, JwtAlgorithm.allECDSA())

  token.tap(println)
  decoded.tap(println)
