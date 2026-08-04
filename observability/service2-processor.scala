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
import io.opentelemetry.api.metrics.LongCounter
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
import sttp.tapir.server.interceptor.RequestInterceptor
import sttp.tapir.server.netty.sync.{NettySyncServer, NettySyncServerOptions}
import sttp.tapir.server.interceptor.log.DefaultServerLog
import sttp.tapir.server.tracing.opentelemetry.OpenTelemetryTracing
import sttp.shared.Identity

import java.net.InetAddress
import java.time.Duration
import scala.collection.mutable

// --- Domain ---

case class ProcessRequest(n: Int, requestedBy: String)
case class FibRequest(n: Int)
case class FibResult(n: Int, result: Long, computedBy: String)
case class ProcessResult(n: Int, result: Long, steps: List[String])

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

  // W3CTraceContextPropagator: wird von OpenTelemetryTracing für das automatische
  // Extract des "traceparent"-Headers aus eingehenden Requests verwendet.
  OpenTelemetrySdk.builder()
    .setTracerProvider(tracerProvider)
    .setMeterProvider(meterProvider)
    .setLoggerProvider(loggerProvider)
    .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
    .buildAndRegisterGlobal()

// --- Main ---

@main
def service2Processor(): Unit =
  val port      = 8082
  val svcName   = "service2-processor"
  val svc3Url   = sys.env.getOrElse("SERVICE3_URL", "http://localhost:8083")
  val otel      = setupOtel(svcName)
  val meter     = otel.getMeter(svcName)
  val otelLogger = otel.getLogsBridge.get(svcName)
  val backend   = DefaultSyncBackend()

  // === OX: ForkLocal für den Hostnamen ===
  val hostLocal = ForkLocal("unknown-host")
  val hostname  = InetAddress.getLocalHost.getHostName

  val requestCounter: LongCounter = meter
    .counterBuilder("processor.requests.total")
    .setDescription("Total processed requests")
    .build()

  def logInfo(msg: String): Unit =
    otelLogger.logRecordBuilder().setSeverity(Severity.INFO).setBody(msg)
      .setAttribute(AttributeKey.stringKey("host.name"), hostLocal.get()).emit()
    println(s"[INFO][$svcName][${hostLocal.get()}] $msg")

  def logWarn(msg: String): Unit =
    otelLogger.logRecordBuilder().setSeverity(Severity.WARN).setBody(msg)
      .setAttribute(AttributeKey.stringKey("host.name"), hostLocal.get()).emit()
    println(s"[WARN][$svcName][${hostLocal.get()}] $msg")

  // TextMapSetter: schreibt den "traceparent"-Header für den ausgehenden Request zu Service3.
  val setter: TextMapSetter[mutable.Map[String, String]] = (carrier, key, value) => carrier.put(key, value)

  // === TAPIR SERVER LOG INTERCEPTOR ===
  // Loggt eingehende Requests mit allen Headern - zeigt den "traceparent" den Service1 gesetzt hat.
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
  def makeHeaderInterceptor(): RequestInterceptor[Identity] =
    RequestInterceptor.transformServerRequest[Identity]: req =>
      val hasTraceParent = req.headers.exists(_.name.toLowerCase == "traceparent")
      if !hasTraceParent then
        logWarn("Kein 'traceparent'-Header - Service direkt aufgerufen, kein Upstream-Trace")
      else
        logInfo(s"traceparent: ${req.headers.find(_.name.toLowerCase == "traceparent").get.value}")
      req

  val processEndpoint = endpoint.post
    .in("process")
    .in(jsonBody[ProcessRequest])
    .out(jsonBody[ProcessResult])
    .errorOut(stringBody)

  val processServerEndpoint = processEndpoint.handle: req =>
    logInfo(s"Processing request from '${req.requestedBy}' for n=${req.n}")
    requestCounter.add(1)

    // === CONTEXT PROPAGATION: Inject ===
    // Der OpenTelemetryTracing-Interceptor hat den eingehenden "traceparent" bereits
    // extrahiert und den zugehörigen Span als io.opentelemetry.context.Context.current()
    // gesetzt. Wir lesen diesen aktiven Context und schreiben ihn als Header in den
    // ausgehenden Request zu Service3 - so entsteht die durchgehende Trace-Kette.
    val outHeaders = mutable.Map.empty[String, String]
    otel.getPropagators.getTextMapPropagator
      .inject(io.opentelemetry.context.Context.current(), outHeaders, setter)

    val response = basicRequest
      .post(uri"$svc3Url/fibonacci")
      .headers(outHeaders.toMap)
      .body(asJson(FibRequest(req.n)))
      .response(asJson[FibResult])
      .send(backend)

    response.body match
      case Right(fibResult) =>
        logInfo(s"Received from service3: fibonacci(${req.n}) = ${fibResult.result}")
        val steps = List(
          s"service2-processor: received request from '${req.requestedBy}'",
          s"service2-processor: forwarded to service3-calculator",
          s"service3-calculator: computed fibonacci(${req.n}) = ${fibResult.result}",
        )
        Right(ProcessResult(req.n, fibResult.result, steps))
      case Left(responseErr) =>
        Left(s"service3 error: $responseErr")

  // === TAPIR: OpenTelemetryTracing-Interceptor ===
  // Übernimmt automatisch:
  //   - Extract des "traceparent"-Headers aus dem eingehenden Request (W3C)
  //   - Span erstellen mit dem extrahierten Parent → korrekte Einordnung in den Trace von Service1
  //   - Span-Attribute: HTTP-Methode, Pfad, Status-Code
  //   - Context aktivieren (makeCurrent) → io.opentelemetry.context.Context.current() liefert
  //     diesen Span im Handler, was das inject() für den Aufruf zu Service3 ermöglicht
  val tapirOptions = NettySyncServerOptions.customiseInterceptors
    .prependInterceptor(OpenTelemetryTracing(otel))
    .serverLog(serverLog)
    .prependInterceptor(makeHeaderInterceptor())
    .options

  println(s"$svcName starting on port $port (host: $hostname)...")

  hostLocal.supervisedWhere(hostname) { (scope: Ox) ?=>
    logInfo(s"$svcName starting on port $port")
    val binding = NettySyncServer(tapirOptions)
      .port(port)
      .addEndpoint(processServerEndpoint)
      .start()
    println(s"$svcName running. Press ENTER to stop...")
    scala.io.StdIn.readLine()
    binding.stop()
  }

  otel.close()
