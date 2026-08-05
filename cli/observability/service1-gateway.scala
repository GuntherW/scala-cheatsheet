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
import io.opentelemetry.api.metrics.{LongCounter, LongHistogram}
import org.slf4j.LoggerFactory
import sttp.client4.*
import sttp.client4.circe.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

import java.net.InetAddress

// --- Domain ---

case class GatewayRequest(n: Int)
case class ProcessRequest(n: Int, requestedBy: String)
case class ProcessResult(n: Int, result: Long, steps: List[String])
case class GatewayResponse(n: Int, fibonacci: Long, traceSteps: List[String], durationMs: Long)

// --- Main ---

@main
def service1Gateway(): Unit =
  val port      = 8081
  val svcName   = "service1-gateway"
  val svc2Url   = sys.env.getOrElse("SERVICE2_URL", "http://localhost:8082")
  val otel      = setupOtel(svcName)
  val meter     = otel.getMeter(svcName)
  val backend   = DefaultSyncBackend()
  val log       = LoggerFactory.getLogger(svcName)

  val requestCounter: LongCounter = meter
    .counterBuilder("gateway.requests.total")
    .setDescription("Total gateway requests")
    .build()

  val e2eLatency: LongHistogram = meter
    .histogramBuilder("gateway.e2e.duration.ms")
    .setDescription("End-to-end request latency in ms")
    .ofLongs()
    .build()

  val fibEndpoint = endpoint.post
    .in("fibonacci")
    .in(jsonBody[GatewayRequest])
    .out(jsonBody[GatewayResponse])
    .errorOut(stringBody)
    .description("Compute Fibonacci(n) via the service chain: gateway -> processor -> calculator")

  val fibServerEndpoint = fibEndpoint.handle: req =>
    val start = System.currentTimeMillis()
    log.info("Gateway received request: fibonacci({})", req.n)
    requestCounter.add(1)

    basicRequest
      .post(uri"$svc2Url/process")
      .headers(otel.currentTraceHeaders)
      .body(asJson(ProcessRequest(req.n, svcName)))
      .response(asJson[ProcessResult])
      .send(backend)
      .body match
        case Right(proc) =>
          val duration = System.currentTimeMillis() - start
          e2eLatency.record(duration)
          log.info("Request complete: fibonacci({}) = {}, total {}ms", req.n, proc.result, duration)
          Right(GatewayResponse(req.n, proc.result, proc.steps, duration))
        case Left(err) =>
          log.warn("Downstream error: {}", err)
          Left(s"service2 error: $err")

  println(s"""
    |=========================================
    | $svcName starting on port $port
    |
    | Example:
    |   curl -X POST http://localhost:$port/fibonacci \\
    |        -H 'Content-Type: application/json' \\
    |        -d '{"n": 10}'
    |=========================================
    |""".stripMargin)

  runServer(port, svcName, otel, fibServerEndpoint)(log)
  otel.close()
