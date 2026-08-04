//> using scala 3.8.4
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
//> using dep org.slf4j:slf4j-simple:2.0.18

import io.circe.generic.auto.*
import io.opentelemetry.api.common.{AttributeKey, Attributes}
import io.opentelemetry.api.logs.Severity
import io.opentelemetry.api.metrics.{LongCounter, LongHistogram}
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.{ContextPropagators, TextMapSetter}
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.`export`.BatchLogRecordProcessor
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.`export`.PeriodicMetricReader
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.`export`.BatchSpanProcessor
import io.opentelemetry.semconv.ServiceAttributes
import ox.{ForkLocal, Ox}
import sttp.client4.*
import sttp.client4.circe.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.netty.sync.{NettySyncServer, NettySyncServerOptions}
import sttp.tapir.server.tracing.opentelemetry.OpenTelemetryTracing

import java.net.InetAddress
import java.time.Duration
import scala.collection.mutable

// --- Domain ---

case class GatewayRequest(n: Int)
case class ProcessRequest(n: Int, requestedBy: String)
case class ProcessResult(n: Int, result: Long, steps: List[String])
case class GatewayResponse(n: Int, fibonacci: Long, traceSteps: List[String], durationMs: Long)

// --- OpenTelemetry Setup ---

def setupOtel(serviceName: String): OpenTelemetrySdk =
  val resource = Resource
    .getDefault()
    .merge(Resource.create(Attributes.of(ServiceAttributes.SERVICE_NAME, serviceName)))

  val otlpEndpoint = sys.env.getOrElse("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4318")

  val tracerProvider = SdkTracerProvider.builder()
    .addSpanProcessor(BatchSpanProcessor.builder(
      OtlpHttpSpanExporter.builder().setEndpoint(s"$otlpEndpoint/v1/traces").build()
    ).build())
    .setResource(resource)
    .build()

  val meterProvider = SdkMeterProvider.builder()
    .registerMetricReader(PeriodicMetricReader.builder(
      OtlpHttpMetricExporter.builder().setEndpoint(s"$otlpEndpoint/v1/metrics").build()
    ).setInterval(Duration.ofSeconds(15)).build())
    .setResource(resource)
    .build()

  val loggerProvider = SdkLoggerProvider.builder()
    .addLogRecordProcessor(BatchLogRecordProcessor.builder(
      OtlpHttpLogRecordExporter.builder().setEndpoint(s"$otlpEndpoint/v1/logs").build()
    ).build())
    .setResource(resource)
    .build()

  // W3CTraceContextPropagator: kodiert TraceId + SpanId als "traceparent"-HTTP-Header.
  // Wird von OpenTelemetryTracing für Extract (eingehend) und im Handler für Inject
  // (ausgehend zu Service2) verwendet.
  OpenTelemetrySdk.builder()
    .setTracerProvider(tracerProvider)
    .setMeterProvider(meterProvider)
    .setLoggerProvider(loggerProvider)
    .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
    .buildAndRegisterGlobal()

// --- Main ---

@main
def service1Gateway(): Unit =
  val port      = 8081
  val svcName   = "service1-gateway"
  val svc2Url   = sys.env.getOrElse("SERVICE2_URL", "http://localhost:8082")
  val otel      = setupOtel(svcName)
  val meter     = otel.getMeter(svcName)
  val otelLogger = otel.getLogsBridge.get(svcName)
  val backend   = DefaultSyncBackend()

  // === OX: ForkLocal für den Hostnamen ===
  val hostLocal = ForkLocal("unknown-host")
  val hostname  = InetAddress.getLocalHost.getHostName

  val requestCounter: LongCounter = meter
    .counterBuilder("gateway.requests.total")
    .setDescription("Total gateway requests")
    .build()

  val e2eLatency: LongHistogram = meter
    .histogramBuilder("gateway.e2e.duration.ms")
    .setDescription("End-to-end request latency in ms")
    .ofLongs()
    .build()

  def logInfo(msg: String): Unit =
    otelLogger.logRecordBuilder().setSeverity(Severity.INFO).setBody(msg)
      .setAttribute(AttributeKey.stringKey("host.name"), hostLocal.get()).emit()
    println(s"[INFO][$svcName][${hostLocal.get()}] $msg")

  // TextMapSetter: schreibt den "traceparent"-Header in die Map für den ausgehenden Request.
  val setter: TextMapSetter[mutable.Map[String, String]] = (carrier, key, value) => carrier.put(key, value)

  val fibEndpoint = endpoint.post
    .in("fibonacci")
    .in(jsonBody[GatewayRequest])
    .out(jsonBody[GatewayResponse])
    .errorOut(stringBody)
    .description("Compute Fibonacci(n) via the service chain: gateway -> processor -> calculator")

  val fibServerEndpoint = fibEndpoint.handle: req =>
    val start = System.currentTimeMillis()
    logInfo(s"Gateway received request: fibonacci(${req.n})")
    requestCounter.add(1)

    // === CONTEXT PROPAGATION: Inject ===
    // Den aktuellen Context (vom OpenTelemetryTracing-Interceptor gesetzt) in HTTP-Header
    // einbetten. io.opentelemetry.context.Context.current() liefert den aktiven Span,
    // den der Interceptor bereits für diesen Request erstellt und aktiviert hat.
    val outHeaders = mutable.Map.empty[String, String]
    otel.getPropagators.getTextMapPropagator
      .inject(io.opentelemetry.context.Context.current(), outHeaders, setter)

    val response = basicRequest
      .post(uri"$svc2Url/process")
      .headers(outHeaders.toMap)
      .body(asJson(ProcessRequest(req.n, svcName)))
      .response(asJson[ProcessResult])
      .send(backend)

    response.body match
      case Right(proc) =>
        val duration = System.currentTimeMillis() - start
        e2eLatency.record(duration)
        logInfo(s"Request complete: fibonacci(${req.n}) = ${proc.result}, total ${duration}ms")
        Right(GatewayResponse(req.n, proc.result, proc.steps, duration))
      case Left(err) =>
        Left(s"service2 error: $err")

  // === TAPIR: OpenTelemetryTracing-Interceptor ===
  // Dieser Interceptor übernimmt das gesamte Tracing automatisch:
  //   - Extract: liest "traceparent" aus eingehenden Headern (hier nicht nötig, da Root-Service)
  //   - Span erstellen: mit Methode, Pfad, Status als Attribute
  //   - makeCurrent: Span in den Thread-Context setzen, damit io.opentelemetry.context.Context.current()
  //     im Handler den aktiven Span liefert - Voraussetzung für das inject() oben
  //   - span.end(): automatisch nach dem Handler
  // Das ersetzt den gesamten manuellen span/scope/try/finally-Block.
  val tapirOptions = NettySyncServerOptions.customiseInterceptors
    .prependInterceptor(OpenTelemetryTracing(otel))
    .options

  println(s"$svcName starting on port $port (host: $hostname)")

  hostLocal.supervisedWhere(hostname) { (scope: Ox) ?=>
    logInfo(s"$svcName starting on port $port")
    val binding = NettySyncServer(tapirOptions)
      .port(port)
      .addEndpoint(fibServerEndpoint)
      .start()
    println(s"$svcName running. Press ENTER to stop...")
    scala.io.StdIn.readLine()
    binding.stop()
  }

  otel.close()
