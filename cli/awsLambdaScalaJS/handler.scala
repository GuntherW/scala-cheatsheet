//> using scala "3.8.4"
//> using platform "js"
//> using jsModuleKind "commonjs"
//> using dep "org.scala-js::scalajs-dom::2.8.1"

import scala.scalajs.js
import scala.scalajs.js.annotation.*

// AWS Lambda Event types (API Gateway v2 / Function URL format)
@js.native
trait LambdaEvent extends js.Object:
  val requestContext: js.UndefOr[js.Dynamic]     = js.native
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

// Response type for API Gateway / Function URL
class LambdaResponse(val statusCode: Int, val headers: js.Dictionary[String], val body: String) extends js.Object

@JSExportTopLevel("handler")
def handler(event: LambdaEvent, context: LambdaContext): js.Promise[LambdaResponse] =
  js.Promise.resolve[LambdaResponse] {
    val path    = event.rawPath.getOrElse("/")
    val name    = extractName(event.rawQueryString.getOrElse(""))
    val message = s"Hello, $name! You called: $path (requestId: ${context.awsRequestId})"

    println(s"[Lambda] function=${context.functionName} requestId=${context.awsRequestId} path=$path")

    new LambdaResponse(
      statusCode = 200,
      headers = js.Dictionary(
        "Content-Type" -> "application/json",
        "X-Powered-By" -> "Scala 3 / Scala.js"
      ),
      body = s"""{"message":"$message","path":"$path","name":"$name"}"""
    )
  }

private def extractName(queryString: String): String =
  queryString
    .split("&")
    .map(_.split("=", 2))
    .collectFirst { case Array("name", v) => v }
    .getOrElse("World")
