package de.wittig.sttp
import sttp.client3.{Request, *}
import sttp.model.*
import java.net.URLEncoder

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.Base64
import java.time.Instant

import scala.util.Random

@main
def oAuth1ByHand(): Unit =

  val backend = HttpURLConnectionBackend()

  val consumerKey        = "yourConsumerKey"
  val accessToken        = "yourAccessToken"
  val consumerPrivateKey = "yourConsumerPrivateKey"
  val accessTokenSecret  = "yourAccessTokenSecret"
  val signer             = Signer(consumerKey, accessToken, consumerPrivateKey, accessTokenSecret)

  val request       = basicRequest.get(uri"https://httpbin.org/get")
  val requestSigned = signer.sign(request)

  println(requestSigned.header("Authorization"))

  // Handle response
  requestSigned.send(backend).body match {
    case Left(error) => println(s"Error: $error")
    case Right(body) => println(s"Success: $body")
  }

class Signer(consumerKey: String, accessToken: String, consumerPrivateKey: String, accessTokenSecret: String):

  private val mac           = Mac.getInstance("HmacSHA1")
  private val signingKey    = s"${encode(consumerPrivateKey)}&${encode(accessTokenSecret)}"
  private val secretKeySpec = new SecretKeySpec(signingKey.getBytes("UTF-8"), "HmacSHA1")
  mac.init(secretKeySpec)

  def sign[L, A](request: Request[Either[L, A], Any]): Request[Either[L, A], Any] =
    val method              = request.method
    val oauthParams         = generateOAuthParams()
    val baseString          = createBaseString(method.method, request.uri, oauthParams)
    val signature           = generateSignature(baseString, secretKeySpec)
    val signedParams        = oauthParams + ("oauth_signature" -> signature)
    val authorizationHeader = "OAuth " + generateAuthorizationHeader(signedParams)
    request.copy(headers = request.headers :+ Header("Authorization", authorizationHeader))

  private def generateOAuthParams(): Map[String, String] =
    Map(
      "oauth_consumer_key"     -> consumerKey,
      "oauth_token"            -> accessToken,
      "oauth_signature_method" -> "HMAC-SHA1",
      "oauth_timestamp"        -> Instant.now.getEpochSecond.toString,
      "oauth_nonce"            -> Random.alphanumeric.take(8).mkString,
      "oauth_version"          -> "1.0"
    )

  // URL encode according to OAuth spec
  private def encode(value: String): String = URLEncoder.encode(value, "UTF-8")

  // Create base string for signing
  private def createBaseString(method: String, url: Uri, params: Map[String, String]): String = {
    val sortedParams = params.toSeq.sortBy(_._1).map((k, v) => s"${encode(k)}=${encode(v)}").mkString("&")
    s"${method.toUpperCase}&${encode(url.toString)}&${encode(sortedParams)}"
  }

  // Generate signature using HMAC-SHA1
  private def generateSignature(baseString: String, secretKeySpec: SecretKeySpec): String =
    val rawSignature = mac.doFinal(baseString.getBytes("UTF-8"))
    Base64.getEncoder.encodeToString(rawSignature)

  // Generate Authorization header
  private def generateAuthorizationHeader(params: Map[String, String]): String =
    params.map((k, v) => s"""$k="${encode(v)}"""").mkString(", ")
