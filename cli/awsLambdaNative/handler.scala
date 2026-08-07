//> using scala "3.3.8"
//> using platform "native"
//> using nativeVersion "0.5.12"
//> using nativeMode "release-fast"
//> using nativeGc "immix"
//> using dep "com.softwaremill.sttp.client4::core_native0.5:4.0.26"

import sttp.client4.*
import sttp.client4.curl.CurlBackend

// AWS Lambda Custom Runtime Bootstrap
// Kommuniziert via Long-Poll mit dem Lambda Runtime API (blockierendes GET bis Event eintrifft)
@main def bootstrap(): Unit =
  val runtimeApi = sys.env.getOrElse("AWS_LAMBDA_RUNTIME_API", "localhost:9001")
  val baseUrl    = s"http://$runtimeApi/2018-06-01/runtime"

  System.err.println(s"[Native Lambda] Starting, runtime API: $runtimeApi")

  val backend = CurlBackend()

  try
    Iterator
      .continually(nextInvocation(baseUrl, backend))
      .foreach: (requestId, body) =>
        postResponse(baseUrl, requestId, handleEvent(body), backend)
  finally
    backend.close()

def nextInvocation(baseUrl: String, backend: SyncBackend): (String, String) =
  val response = basicRequest
    .get(uri"$baseUrl/invocation/next")
    .response(asStringAlways)
    .send(backend)

  val requestId = response.header("Lambda-Runtime-Aws-Request-Id").getOrElse("unknown")
  (requestId, response.body)

def postResponse(baseUrl: String, requestId: String, body: String, backend: SyncBackend): Unit =
  basicRequest
    .post(uri"$baseUrl/invocation/$requestId/response")
    .contentType("application/json")
    .body(body)
    .response(asStringAlways)
    .send(backend)
  ()

def handleEvent(body: String): String =
  val name = extractQueryParam(body, "name").getOrElse("World")
  val path = extractJsonField(body, "rawPath").getOrElse("/")

  System.err.println(s"[Native Lambda] path=$path name=$name")

  s"""|{
      |  "statusCode": 200,
      |  "headers": {
      |    "Content-Type": "application/json",
      |    "X-Powered-By": "Scala 3.3.8 / Scala Native 0.5.12 / sttp"
      |  },
      |  "body": "{\\"message\\":\\"Hello, $name! You called: $path\\",\\"path\\":\\"$path\\",\\"name\\":\\"$name\\"}"
      |}""".stripMargin

def extractQueryParam(json: String, param: String): Option[String] =
  extractJsonField(json, "rawQueryString").flatMap: qs =>
    qs.split("&")
      .map(_.split("=", 2))
      .collectFirst { case Array(k, v) if k == param => v }

def extractJsonField(json: String, field: String): Option[String] =
  val key = s""""$field":""""
  val idx = json.indexOf(key)
  Option.when(idx >= 0):
    val start = idx + key.length
    json.substring(start, json.indexOf('"', start))
