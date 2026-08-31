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
import ox.raceEither
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
  val port    = 8082
  val svcName = "service2-processor"
  val svc3Url = sys.env.getOrElse("SERVICE3_URL", "http://localhost:8083")
  val otel    = setupOtel(svcName)
  val meter   = otel.getMeter(svcName)
  val backend = DefaultSyncBackend()
  val log     = LoggerFactory.getLogger(svcName)

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

    // Den aktuellen OTel-Context VOR raceEither auf dem Handler-Thread lesen.
    // raceEither startet intern neue Virtual Threads (via ox.unsupervised) - der OTel
    // Context ist thread-lokal und wird nicht automatisch übertragen. Deshalb den
    // Context hier einmal lesen und als unveränderliche Map in beide Lambdas schließen.
    val traceHeaders = otel.currentTraceHeaders

    // Hilfsfunktion: eine Anfrage an Service3 abschicken.
    // Exceptions werden in Left gewandelt - raceEither erwartet Either, keine Exceptions.
    def callService3(attempt: Int): Either[String, FibResult] =
      log.debug("Calling service3, attempt {}", attempt)
      try
        basicRequest
          .post(uri"$svc3Url/fibonacci")
          .headers(traceHeaders) // vorgefangener Context - korrekte traceId auf jedem Thread
          .body(asJson(FibRequest(req.n)))
          .response(asJson[FibResult])
          .send(backend)
          .body
          .left
          .map(err => s"attempt $attempt: $err")
      catch case ex: Exception => Left(s"attempt $attempt: ${ex.getMessage}")

    // === OX: raceEither ===
    // Startet zwei identische Anfragen an Service3 parallel (auf je einem Virtual Thread).
    // Die erste Anfrage die ein Right liefert gewinnt - die andere wird sofort abgebrochen.
    // Liefern beide Left, wird das Left des letzten Verlierers zurückgegeben.
    // Typischer Anwendungsfall: Hedged Requests - Latenz-Ausreißer absichern,
    // indem man eine Duplikat-Anfrage leicht verzögert nachschickt und die schnellste Antwort nimmt.
    raceEither(callService3(1), callService3(2)) match
      case Right(result) =>
        log.info("Received from service3: fibonacci({}) = {}", req.n, result.result)
        Right(
          ProcessResult(
            n = req.n,
            result = result.result,
            steps = List(
              s"service2-processor: received request from '${req.requestedBy}'",
              s"service2-processor: forwarded to service3-calculator (raced 2 requests)",
              s"service3-calculator: computed fibonacci(${req.n}) = ${result.result}"
            )
          )
        )
      case Left(errMsg)  =>
        log.warn("Downstream error from service3: {}", errMsg)
        Left(s"service3 error: $errMsg")

  runServer(port, svcName, otel, processServerEndpoint)(log)
  otel.close()
