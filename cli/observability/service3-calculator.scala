//> using file otel.scala
//> using resourceDir .
//> using dep com.softwaremill.sttp.tapir::tapir-netty-server-sync:1.13.30
//> using dep com.softwaremill.sttp.tapir::tapir-json-circe:1.13.30
//> using dep com.softwaremill.sttp.tapir::tapir-opentelemetry-tracing:1.13.30
//> using dep io.circe::circe-generic:0.14.16
//> using dep io.opentelemetry:opentelemetry-api:1.64.0
//> using dep io.opentelemetry:opentelemetry-sdk:1.64.0
//> using dep io.opentelemetry:opentelemetry-exporter-otlp:1.64.0
//> using dep io.opentelemetry.semconv:opentelemetry-semconv:1.44.0
//> using dep io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.30.0-alpha
//> using dep ch.qos.logback:logback-classic:1.6.1

import io.circe.generic.auto.*
import io.opentelemetry.api.common.{AttributeKey, Attributes}
import io.opentelemetry.api.metrics.{LongCounter, LongHistogram}
import org.slf4j.LoggerFactory
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

// --- Domain ---

case class FibRequest(n: Int)
case class FibResult(n: Int, result: Long, computedBy: String)

// --- Fibonacci ---

def fibonacci(n: Int): Long =
  Iterator.iterate((0L, 1L))((a, b) => (b, a + b)).drop(n).next()._1

// --- Main ---

@main
def service3Calculator(): Unit =
  val port    = 8083
  val svcName = "service3-calculator"
  val otel    = setupOtel(svcName)
  val meter   = otel.getMeter(svcName)
  val log     = LoggerFactory.getLogger(svcName)

  val requestCounter: LongCounter = meter
    .counterBuilder("fibonacci.requests.total")
    .setDescription("Total number of Fibonacci requests")
    .build()

  val latencyHistogram: LongHistogram = meter
    .histogramBuilder("fibonacci.computation.duration.ms")
    .setDescription("Duration of Fibonacci computation in ms")
    .ofLongs()
    .build()

  val fibEndpoint = endpoint.post
    .in("fibonacci")
    .in(jsonBody[FibRequest])
    .out(jsonBody[FibResult])
    .errorOut(stringBody)

  val fibServerEndpoint = fibEndpoint.handle: req =>
    if req.n < 0 || req.n > 90 then
      log.warn("Input out of range: {}", req.n)
      Left(s"n must be between 0 and 90, got ${req.n}")
    else
      val start    = System.currentTimeMillis()
      val result   = fibonacci(req.n)
      val duration = System.currentTimeMillis() - start
      requestCounter.add(1, Attributes.of(AttributeKey.longKey("input"), req.n.toLong))
      latencyHistogram.record(duration)
      log.info("fibonacci({}) = {} (took {}ms)", req.n, result, duration)
      Right(FibResult(req.n, result, svcName))

  runServer(port, svcName, otel, fibServerEndpoint)(log)
  otel.close()
