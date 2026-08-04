//> using scala 3.8.4
//> using file otel.scala
//> using resourceDir .
//> using dep com.softwaremill.sttp.tapir::tapir-netty-server-sync:1.13.30
//> using dep com.softwaremill.sttp.tapir::tapir-json-circe:1.13.30
//> using dep com.softwaremill.sttp.tapir::tapir-opentelemetry-tracing:1.13.30
//> using dep com.softwaremill.sttp.client4::core:4.0.26
//> using dep com.softwaremill.sttp.client4::circe:4.0.26
//> using dep io.circe::circe-generic:0.14.16
//> using dep io.opentelemetry:opentelemetry-api:1.64.0
//> using dep io.opentelemetry:opentelemetry-sdk:1.64.0
//> using dep io.opentelemetry:opentelemetry-exporter-otlp:1.64.0
//> using dep io.opentelemetry.semconv:opentelemetry-semconv:1.43.0
//> using dep io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.30.0-alpha
//> using dep ch.qos.logback:logback-classic:1.6.1

import io.circe.generic.auto.*
import io.opentelemetry.api.metrics.LongCounter
import org.slf4j.LoggerFactory
import ox.ForkLocal
import sttp.client4.*
import sttp.client4.circe.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

import java.net.InetAddress

// --- Domain ---

case class ProcessRequest(n: Int, requestedBy: String)
case class FibRequest(n: Int)
case class FibResult(n: Int, result: Long, computedBy: String)
case class ProcessResult(n: Int, result: Long, steps: List[String])

// --- Main ---

@main
def service2Processor(): Unit =
  val port      = 8082
  val svcName   = "service2-processor"
  val svc3Url   = sys.env.getOrElse("SERVICE3_URL", "http://localhost:8083")
  val otel      = setupOtel(svcName)
  val meter     = otel.getMeter(svcName)
  val hostLocal = ForkLocal("unknown-host")
  val hostname  = InetAddress.getLocalHost.getHostName
  val backend   = DefaultSyncBackend()
  val log       = LoggerFactory.getLogger(svcName)

  val requestCounter: LongCounter = meter
    .counterBuilder("processor.requests.total")
    .setDescription("Total processed requests")
    .build()

  val processEndpoint = endpoint.post
    .in("process")
    .in(jsonBody[ProcessRequest])
    .out(jsonBody[ProcessResult])
    .errorOut(stringBody)

  val processServerEndpoint = processEndpoint.handle: req =>
    log.info("Processing request from '{}' for n={}", req.requestedBy, req.n)
    requestCounter.add(1)

    basicRequest
      .post(uri"$svc3Url/fibonacci")
      .headers(otel.currentTraceHeaders)
      .body(asJson(FibRequest(req.n)))
      .response(asJson[FibResult])
      .send(backend)
      .body match
        case Right(fibResult) =>
          log.info("Received from service3: fibonacci({}) = {}", req.n, fibResult.result)
          Right(ProcessResult(
            n      = req.n,
            result = fibResult.result,
            steps  = List(
              s"service2-processor: received request from '${req.requestedBy}'",
              s"service2-processor: forwarded to service3-calculator",
              s"service3-calculator: computed fibonacci(${req.n}) = ${fibResult.result}",
            ),
          ))
        case Left(err) =>
          log.warn("Downstream error from service3: {}", err)
          Left(s"service3 error: $err")

  println(s"$svcName starting on port $port (host: $hostname)...")
  runServer(port, svcName, otel, hostLocal, hostname, processServerEndpoint)(log)
  otel.close()
