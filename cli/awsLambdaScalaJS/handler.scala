//> using scala "3.9.0"
//> using platform "js"
//> using jsModuleKind "commonjs"
//> using dep "org.scala-js::scalajs-dom::2.8.1"

import scala.scalajs.js
import scala.scalajs.js.annotation.*

// AWS Lambda Event (API Gateway v2 / Function URL)
@js.native
trait LambdaEvent extends js.Object:
  val rawPath: js.UndefOr[String]                = js.native
  val rawQueryString: js.UndefOr[String]         = js.native
  val headers: js.UndefOr[js.Dictionary[String]] = js.native
  val body: js.UndefOr[String]                   = js.native
  val isBase64Encoded: js.UndefOr[Boolean]       = js.native

@js.native
trait LambdaContext extends js.Object:
  val functionName: String    = js.native
  val functionVersion: String = js.native
  val awsRequestId: String    = js.native
  val memoryLimitInMB: String = js.native

class LambdaResponse(
  val statusCode: Int,
  val headers: js.Dictionary[String],
  val body: String
) extends js.Object

@JSExportTopLevel("handler")
def handler(event: LambdaEvent, context: LambdaContext): js.Promise[LambdaResponse] =
  val path = event.rawPath.getOrElse("/")
  val name = event.rawQueryString
    .toOption
    .flatMap(extractQueryParam(_, "name"))
    .getOrElse("World")

  println(s"[Lambda] function=${context.functionName} requestId=${context.awsRequestId} path=$path")

  val responseBody = s"""{"message":"Hello, $name! You called: $path","path":"$path","name":"$name"}"""

  js.Promise.resolve[LambdaResponse]:
    new LambdaResponse(
      statusCode = 200,
      headers    = js.Dictionary(
        "Content-Type" -> "application/json",
        "X-Powered-By" -> "Scala 3 / Scala.js"
      ),
      body = responseBody
    )

private def extractQueryParam(queryString: String, param: String): Option[String] =
  queryString
    .split("&")
    .map(_.split("=", 2))
    .collectFirst { case Array(k, v) if k == param => v }
