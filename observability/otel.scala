// Gemeinsame OpenTelemetry-Infrastruktur für alle drei Services.
// Eingebunden per: //> using file otel.scala

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.`export`.BatchLogRecordProcessor
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.`export`.PeriodicMetricReader
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.`export`.BatchSpanProcessor
import io.opentelemetry.semconv.ServiceAttributes
import org.slf4j.{Logger, LoggerFactory, MDC}
import ox.{ForkLocal, Ox}
import sttp.tapir.server.interceptor.log.DefaultServerLog
import sttp.tapir.server.netty.sync.{NettySyncServer, NettySyncServerOptions}
import sttp.tapir.server.tracing.opentelemetry.OpenTelemetryTracing
import sttp.tapir.server.ServerEndpoint
import sttp.shared.Identity

import java.time.Duration

// --- OTel SDK Setup ---

def setupOtel(serviceName: String): OpenTelemetrySdk =
  val resource     = Resource.getDefault.merge(
    Resource.create(Attributes.of(ServiceAttributes.SERVICE_NAME, serviceName))
  )
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

  val otel = OpenTelemetrySdk.builder()
    .setTracerProvider(tracerProvider)
    .setMeterProvider(meterProvider)
    .setLoggerProvider(loggerProvider)
    .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
    .buildAndRegisterGlobal()

  // Den OTel Logback Appender mit dem gerade erstellten SDK verknüpfen.
  // Ab jetzt leitet Logback alle Log-Einträge automatisch ans OTel SDK weiter
  // (zusätzlich zur Konsolen-Ausgabe via ConsoleAppender in logback.xml).
  OpenTelemetryAppender.install(otel)
  otel

// --- Tapir Server Log ---

// Tapir DefaultServerLog via SLF4J - nutzt den übergebenen Logger direkt.
// Alle Level (info/warn/error) und Exception-Überladungen stehen über SLF4J zur Verfügung.
def otelServerLog(log: Logger) =
  NettySyncServerOptions.defaultServerLog
    .showRequest: req =>
      val headersStr = req.headers.map(h => s"  ${h.name}: ${h.value}").mkString("\n")
      s"${req.method} ${req.uri}\nHeaders:\n$headersStr"
    .logWhenReceived(true)
    .doLogWhenReceived(msg => log.info("[tapir] {}", msg))
    .logWhenHandled(true)
    .doLogWhenHandled: (msg, maybeEx) =>
      maybeEx.fold(log.info("[tapir] {}", msg))(ex => log.warn("[tapir] {}", msg, ex))
    .logAllDecodeFailures(true)
    .doLogAllDecodeFailures((msg, maybeEx) =>
      maybeEx.fold(log.warn("[tapir] decode failure: {}", msg))(ex => log.warn("[tapir] decode failure: {}", msg, ex))
    )

// Baut die NettySyncServerOptions mit OTel Tracing und Server-Log zusammen.
def buildServerOptions(otel: OpenTelemetrySdk, log: Logger): NettySyncServerOptions =
  NettySyncServerOptions.customiseInterceptors
    .prependInterceptor(OpenTelemetryTracing(otel))
    .serverLog(otelServerLog(log))
    .options

// --- Server starten ---

// Kapselt den supervisedWhere-Block + NettySyncServer-Lifecycle.
// hostLocal: Ox ForkLocal mit dem Hostnamen - wird per MDC in jeden Log-Eintrag eingebettet,
// sodass Logback (und damit OTel/Loki) den Host automatisch als Attribut bekommt.
def runServer(
  port: Int,
  svcName: String,
  otel: OpenTelemetrySdk,
  hostLocal: ForkLocal[String],
  hostname: String,
  endpoint: ServerEndpoint[Any, Identity],
)(log: Logger): Unit =
  val options = buildServerOptions(otel, log)
  hostLocal.supervisedWhere(hostname) { (scope: Ox) ?=>
    // MDC.put: setzt den Hostnamen als Logback-MDC-Wert für den aktuellen Thread.
    // Logback schreibt ihn automatisch in jede Log-Zeile (Pattern: %X{host}).
    // Der OTel Appender exportiert MDC-Keys als OTel-Attribute (captureKeyValuePairAttributes).
    // Da Ox Virtual Threads verwendet, gilt der MDC-Wert für alle Request-Handler-Threads
    // innerhalb dieses supervisedWhere-Scopes.
    MDC.put("host", hostname)
    log.info("{} starting on port {}", svcName, port)
    val binding = NettySyncServer(options).port(port).addEndpoint(endpoint).start()
    println(s"$svcName running. Press ENTER to stop...")
    scala.io.StdIn.readLine()
    binding.stop()
    MDC.remove("host")
  }

// --- Context Propagation: Inject ---

// Extension method auf OpenTelemetrySdk: liefert die aktuellen Trace-Header als
// immutable Map für ausgehende HTTP-Requests.
extension (otel: OpenTelemetrySdk)
  def currentTraceHeaders: Map[String, String] =
    val carrier = scala.collection.mutable.Map.empty[String, String]
    otel.getPropagators.getTextMapPropagator
      .inject(io.opentelemetry.context.Context.current(), carrier, (c, k, v) => c.put(k, v))
    carrier.toMap
