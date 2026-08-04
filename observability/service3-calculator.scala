//> using scala 3.8.4
//> using dep com.softwaremill.sttp.tapir::tapir-netty-server-sync:1.13.30
//> using dep com.softwaremill.sttp.tapir::tapir-json-circe:1.13.30
//> using dep com.softwaremill.sttp.tapir::tapir-opentelemetry-tracing:1.13.30
//> using dep io.circe::circe-generic:0.14.16
// OpenTelemetry besteht aus drei Teilen:
//   opentelemetry-api  - Interfaces (Tracer, Meter, Logger) - werden im Code verwendet
//   opentelemetry-sdk  - Konkrete Implementierung der API
//   opentelemetry-exporter-otlp - Sendet Daten per OTLP-Protokoll an den Collector
//> using dep io.opentelemetry:opentelemetry-api:1.64.0
//> using dep io.opentelemetry:opentelemetry-sdk:1.64.0
//> using dep io.opentelemetry:opentelemetry-exporter-otlp:1.64.0
// Semantic Conventions: standardisierte Attributnamen (z.B. "service.name")
//> using dep io.opentelemetry.semconv:opentelemetry-semconv:1.43.0
//> using dep org.slf4j:slf4j-simple:2.0.18

import io.circe.generic.auto.*
import io.opentelemetry.api.common.{AttributeKey, Attributes}
import io.opentelemetry.api.logs.Severity
import io.opentelemetry.api.metrics.{LongCounter, LongHistogram}
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
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
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.interceptor.RequestInterceptor
import sttp.tapir.server.interceptor.log.DefaultServerLog
import sttp.tapir.server.netty.sync.{NettySyncServer, NettySyncServerOptions}
import sttp.tapir.server.tracing.opentelemetry.OpenTelemetryTracing
import sttp.shared.Identity

import java.net.InetAddress
import java.time.Duration

// --- Domain ---

case class FibRequest(n: Int)
case class FibResult(n: Int, result: Long, computedBy: String)

// --- OpenTelemetry Setup ---

// Zentrales Setup der drei OpenTelemetry-Signaltypen: Traces, Metriken, Logs.
// Alle drei Signale werden per OTLP (OpenTelemetry Protocol) über HTTP an den
// OTel Collector gesendet, der sie dann an Tempo, Prometheus und Loki weiterleitet.
def setupOtel(serviceName: String): OpenTelemetrySdk =

  // Resource: beschreibt die Quelle der Telemetriedaten (welcher Service sendet).
  // "service.name" ist das wichtigste Attribut - es taucht in Grafana als Label auf
  // und erlaubt die Filterung aller Signale nach Service.
  val resource = Resource
    .getDefault()
    .merge(Resource.create(Attributes.of(ServiceAttributes.SERVICE_NAME, serviceName)))

  // Endpunkt des OTel Collectors. Kann per Umgebungsvariable überschrieben werden.
  val otlpEndpoint = sys.env.getOrElse("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4318")

  // === TRACES ===
  // BatchSpanProcessor puffert Spans und sendet sie gebündelt - reduziert Netzwerklast.
  val tracerProvider = SdkTracerProvider.builder()
    .addSpanProcessor(BatchSpanProcessor.builder(
      OtlpHttpSpanExporter.builder().setEndpoint(s"$otlpEndpoint/v1/traces").setTimeout(Duration.ofSeconds(10)).build()
    ).build())
    .setResource(resource)
    .build()

  // === METRIKEN ===
  // PeriodicMetricReader sammelt alle 15 Sekunden den aktuellen Stand und sendet ihn.
  val meterProvider = SdkMeterProvider.builder()
    .registerMetricReader(PeriodicMetricReader.builder(
      OtlpHttpMetricExporter.builder().setEndpoint(s"$otlpEndpoint/v1/metrics").build()
    ).setInterval(Duration.ofSeconds(15)).build())
    .setResource(resource)
    .build()

  // === LOGS ===
  // OTel-Logs ermöglichen strukturierte Log-Einträge mit Trace-ID-Korrelation.
  val loggerProvider = SdkLoggerProvider.builder()
    .addLogRecordProcessor(BatchLogRecordProcessor.builder(
      OtlpHttpLogRecordExporter.builder().setEndpoint(s"$otlpEndpoint/v1/logs").build()
    ).build())
    .setResource(resource)
    .build()

  // W3CTraceContextPropagator: wird vom OpenTelemetryTracing-Interceptor für das
  // automatische Extract des "traceparent"-Headers aus eingehenden Requests benötigt.
  OpenTelemetrySdk.builder()
    .setTracerProvider(tracerProvider)
    .setMeterProvider(meterProvider)
    .setLoggerProvider(loggerProvider)
    .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
    .buildAndRegisterGlobal()

// --- Fibonacci Logic ---

def fibonacci(n: Int): Long =
  if n <= 1 then n
  else
    var a = 0L
    var b = 1L
    for _ <- 2 to n do
      val tmp = a + b
      a = b
      b = tmp
    b

// --- Main ---

@main
def service3Calculator(): Unit =
  val port      = 8083
  val svcName   = "service3-calculator"
  val otel      = setupOtel(svcName)
  val meter     = otel.getMeter(svcName)
  val otelLogger = otel.getLogsBridge.get(svcName)

  // === OX: ForkLocal für den Hostnamen ===
  // ForkLocal propagiert den Wert automatisch in alle Forks des Ox-Scopes -
  // kein manuelles Weitergeben durch den Call-Stack nötig.
  val hostLocal = ForkLocal("unknown-host")
  val hostname  = InetAddress.getLocalHost.getHostName

  // === METRIK: Counter ===
  // In Prometheus: observability_fibonacci_requests_total
  val requestCounter: LongCounter = meter
    .counterBuilder("fibonacci.requests.total")
    .setDescription("Total number of Fibonacci requests")
    .build()

  // === METRIK: Histogramm ===
  // Ermöglicht Perzentil-Auswertungen (p50, p95, p99) der Berechnungsdauer.
  val latencyHistogram: LongHistogram = meter
    .histogramBuilder("fibonacci.computation.duration.ms")
    .setDescription("Duration of Fibonacci computation in ms")
    .ofLongs()
    .build()

  def logInfo(msg: String): Unit =
    otelLogger.logRecordBuilder().setSeverity(Severity.INFO).setBody(msg)
      .setAttribute(AttributeKey.stringKey("host.name"), hostLocal.get()).emit()
    println(s"[INFO][$svcName][${hostLocal.get()}] $msg")

  // WARN-Level: für unerwartete Situationen, z.B. fehlender traceparent-Header.
  def logWarn(msg: String): Unit =
    otelLogger.logRecordBuilder().setSeverity(Severity.WARN).setBody(msg)
      .setAttribute(AttributeKey.stringKey("host.name"), hostLocal.get()).emit()
    println(s"[WARN][$svcName][${hostLocal.get()}] $msg")

  // === TAPIR SERVER LOG INTERCEPTOR ===
  // Loggt eingehende Requests mit allen Headern.
  // Besonders interessant: "traceparent" - den Service2 per W3C-Propagation gesetzt hat.
  val serverLog = NettySyncServerOptions.defaultServerLog
    .showRequest: req =>
      val headersStr = req.headers.map(h => s"  ${h.name}: ${h.value}").mkString("\n")
      s"${req.method} ${req.uri}\nHeaders:\n$headersStr"
    .logWhenReceived(true)
    .doLogWhenReceived(msg => logInfo(s"[tapir] $msg"))
    .logWhenHandled(true)
    .doLogWhenHandled: (msg, maybeEx) =>
      maybeEx.fold(logInfo(s"[tapir] $msg"))(_ => logWarn(s"[tapir] $msg"))
    .logAllDecodeFailures(true)
    .doLogAllDecodeFailures((msg, _) => logWarn(s"[tapir] decode failure: $msg"))

  // === TAPIR REQUEST INTERCEPTOR: traceparent-Check ===
  // Warnt wenn kein "traceparent"-Header vorhanden - bedeutet Direktaufruf ohne Upstream-Trace.
  def makeHeaderInterceptor(): RequestInterceptor[Identity] =
    RequestInterceptor.transformServerRequest[Identity]: req =>
      val hasTraceParent = req.headers.exists(_.name.toLowerCase == "traceparent")
      if !hasTraceParent then
        logWarn("Kein 'traceparent'-Header - Service direkt aufgerufen, kein Upstream-Trace")
      else
        logInfo(s"traceparent: ${req.headers.find(_.name.toLowerCase == "traceparent").get.value}")
      req

  val fibEndpoint = endpoint.post
    .in("fibonacci")
    .in(jsonBody[FibRequest])
    .out(jsonBody[FibResult])
    .errorOut(stringBody)

  val fibServerEndpoint = fibEndpoint.handle: req =>
    val start = System.currentTimeMillis()
    logInfo(s"Computing fibonacci(${req.n})")
    requestCounter.add(1, Attributes.of(AttributeKey.longKey("input"), req.n.toLong))

    if req.n < 0 || req.n > 90 then
      Left(s"n must be between 0 and 90, got ${req.n}")
    else
      val result   = fibonacci(req.n)
      val duration = System.currentTimeMillis() - start
      latencyHistogram.record(duration)
      logInfo(s"fibonacci(${req.n}) = $result (took ${duration}ms)")
      Right(FibResult(req.n, result, svcName))

  // === TAPIR: OpenTelemetryTracing-Interceptor ===
  // Ersetzt den gesamten manuellen Tracing-Code (span erstellen, makeCurrent,
  // setParent, scope.close, span.end) durch einen einzigen Interceptor.
  // Er übernimmt automatisch:
  //   - Extract: liest "traceparent" aus eingehenden Headern (W3CTraceContextPropagator)
  //   - Span erstellen: mit korrektem Parent → dieser Span erscheint in Tempo als
  //     Kind-Span unter dem Span von Service2
  //   - Attribute: HTTP-Methode, Pfad, Status-Code nach Abschluss
  //   - Context aktivieren und nach dem Handler sauber schließen
  val tapirOptions = NettySyncServerOptions.customiseInterceptors
    .prependInterceptor(OpenTelemetryTracing(otel))
    .serverLog(serverLog)
    .prependInterceptor(makeHeaderInterceptor())
    .options

  println(s"$svcName starting on port $port (host: $hostname)...")

  // supervisedWhere: setzt hostLocal für den gesamten Server-Scope.
  // Alle Request-Handler-Threads erben den Wert automatisch über Ox's ForkLocal.
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
