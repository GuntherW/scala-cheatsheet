package rag.observability

import dev.langchain4j.model.chat.listener.{ChatModelErrorContext, ChatModelListener, ChatModelRequestContext, ChatModelResponseContext}
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.embedding.listener.{EmbeddingModelErrorContext, EmbeddingModelListener, EmbeddingModelRequestContext, EmbeddingModelResponseContext}
import dev.langchain4j.model.embedding.request.EmbeddingRequest
import dev.langchain4j.rag.content.Content
import dev.langchain4j.rag.content.retriever.ContentRetriever
import dev.langchain4j.rag.query.Query
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.{AttributeKey, Attributes}
import io.opentelemetry.api.metrics.{DoubleHistogram, LongCounter}
import io.opentelemetry.api.trace.{Span, StatusCode, Tracer}
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.`export`.PeriodicMetricReader
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.`export`.BatchSpanProcessor

import java.time.Duration
import java.util.List as JList
import java.util.concurrent.TimeUnit

import scala.util.control.NonFatal

/** Traces und Metriken per OpenTelemetry (OTLP/HTTP) nach OpenObserve (`docker compose --profile otel up -d openobserve`, UI http://localhost:5080).
  *
  * Was wird aufgezeichnet:
  *   - Span `rag.ask` / `rag.ingest` (Wurzel eines CLI-Laufs)
  *   - Span `rag.retrieve` (Embedding der Frage + pgvector-Suche, inkl. Anzahl Treffer)
  *   - Span `embeddings <modell>` je Embedding-Aufruf
  *   - Span `chat <modell>` je LLM-Aufruf (Provider, Modell, Tokens, Finish-Reason, Dauer)
  *   - Metriken `gen_ai.client.token.usage` und `gen_ai.client.operation.duration` (OTel-GenAI-Konventionen, Chat und Embeddings)
  *
  * Konfiguration per Umgebungsvariablen (siehe `.env`):
  *   - `OTEL_AUTHORIZATION`: `Basic <base64(email:passwort)>` (Werte aus `docker/.env.observability`)
  *   - `OTEL_ENDPOINT`: Basis-URL inkl. Organisation, Standard `http://localhost:5080/api/default`
  *   - `RAG_OBS_OTEL=false` schaltet alles ab (No-Op).
  *
  * Ist OpenObserve nicht erreichbar, schlägt der Export still im Hintergrund fehl.
  */
object Observability:

  private val serviceName = "ai-rag-langchain4j"

  private val enabled: Boolean = !sys.env.get("RAG_OBS_OTEL").exists(_.equalsIgnoreCase("false"))

  private val authorization: Option[String] = sys.env.get("OTEL_AUTHORIZATION").filter(_.nonEmpty)

  private def buildSdk(): OpenTelemetrySdk =
    val endpoint = sys.env.getOrElse("OTEL_ENDPOINT", "http://localhost:5080/api/default")
    val resource = Resource.getDefault.merge(Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), serviceName)))

    val spanExporter   = authorization.foldLeft(OtlpHttpSpanExporter.builder().setEndpoint(s"$endpoint/v1/traces"))((b, a) => b.addHeader("Authorization", a)).build()
    val metricExporter = authorization.foldLeft(OtlpHttpMetricExporter.builder().setEndpoint(s"$endpoint/v1/metrics"))((b, a) => b.addHeader("Authorization", a)).build()

    val tracerProvider = SdkTracerProvider
      .builder()
      .setResource(resource)
      .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).setScheduleDelay(Duration.ofSeconds(1)).build())
      .build()
    val meterProvider  = SdkMeterProvider
      .builder()
      .setResource(resource)
      .registerMetricReader(PeriodicMetricReader.builder(metricExporter).setInterval(Duration.ofSeconds(10)).build())
      .build()

    OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).setMeterProvider(meterProvider).build()

  /** Nur bei aktivierter Observability wird das echte SDK (mit Exportern/Threads) gebaut - sonst läuft alles gegen `OpenTelemetry.noop()`. */
  private val sdk: Option[OpenTelemetrySdk] = Option.when(enabled)(buildSdk())
  private val otel: OpenTelemetry           = sdk.getOrElse(OpenTelemetry.noop())

  private val tracer: Tracer = otel.getTracer(serviceName)

  private val tokenUsage: LongCounter =
    otel.getMeter(serviceName).counterBuilder("gen_ai.client.token.usage").setUnit("{token}").setDescription("Verbrauchte LLM-Tokens").build()

  private val opDuration: DoubleHistogram =
    otel.getMeter(serviceName).histogramBuilder("gen_ai.client.operation.duration").setUnit("s").setDescription("Dauer eines LLM-Aufrufs").build()

  def status: String =
    if !enabled then "OpenTelemetry: aus (RAG_OBS_OTEL=false)"
    else if authorization.isEmpty then "OpenTelemetry -> OpenObserve: an, ACHTUNG: OTEL_AUTHORIZATION fehlt (Export wird abgelehnt)"
    else "OpenTelemetry -> OpenObserve: an"

  private def recordError(otelSpan: Span, e: Throwable): Unit =
    otelSpan.recordException(e)
    otelSpan.setStatus(StatusCode.ERROR, String.valueOf(e.getMessage))

  /** Führt `body` in einem Span aus; Fehler werden am Span vermerkt und weitergereicht. Kindspans (LLM-Aufrufe) hängen sich automatisch darunter. */
  def span[T](name: String, attributes: (String, String)*)(body: Span => T): T =
    if !enabled then body(Span.getInvalid)
    else
      val otelSpan = tracer.spanBuilder(name).startSpan()
      attributes.foreach((k, v) => otelSpan.setAttribute(k, v))
      val scope    = otelSpan.makeCurrent()
      try body(otelSpan)
      catch
        case NonFatal(e) =>
          recordError(otelSpan, e)
          throw e
      finally
        scope.close()
        otelSpan.end()

  /** Sendet gepufferte Spans/Metriken und beendet das SDK - ohne das gehen bei kurzen CLI-Läufen die letzten Daten verloren. */
  def shutdown(): Unit = sdk.foreach(_.shutdown().join(10, TimeUnit.SECONDS))

  private val SpanKey  = "otel.span"
  private val StartKey = "otel.start"

  private def str(a: Any): String = Option(a).fold("unknown")(_.toString)

  private def startSpan(name: String, operation: String, provider: Any, model: String, attributes: java.util.Map[Object, Object]): Span =
    val span = tracer
      .spanBuilder(name)
      .setAttribute("gen_ai.operation.name", operation)
      .setAttribute("gen_ai.provider.name", str(provider))
      .setAttribute("gen_ai.request.model", model)
      .startSpan()
    attributes.put(SpanKey, span)
    attributes.put(StartKey, Long.box(System.nanoTime()))
    span

  /** Beendet den in `startSpan` angelegten Span (auch wenn `f` wirft) und erfasst die Dauer als `gen_ai.client.operation.duration`. */
  private def finish(attributes: java.util.Map[Object, Object], metricAttrs: Attributes)(f: Span => Unit): Unit =
    Option(attributes.get(SpanKey)).collect { case span: Span => span }.foreach { span =>
      try
        f(span)
        Option(attributes.get(StartKey))
          .collect { case start: java.lang.Long => start.longValue }
          .foreach(start => opDuration.record((System.nanoTime() - start) / 1e9, metricAttrs))
      finally span.end()
    }

  private def metricAttrs(operation: String, provider: Any, requestModel: String, responseModel: Option[String] = None): Attributes =
    val b = Attributes
      .builder()
      .put("gen_ai.operation.name", operation)
      .put("gen_ai.provider.name", str(provider))
      .put("gen_ai.request.model", requestModel)
    responseModel.foreach(b.put("gen_ai.response.model", _))
    b.build()

  private def recordTokens(span: Span, attrs: Attributes, kind: String, count: Option[Int]): Unit =
    count.foreach { t =>
      tokenUsage.add(t.toLong, attrs.toBuilder.put("gen_ai.token.type", kind).build())
      span.setAttribute(s"gen_ai.usage.${kind}_tokens", t.toLong)
    }

  /** Listener für `ChatModel` - ein Span pro LLM-Aufruf, plus Token-/Dauer-Metriken. */
  private val chatListener: ChatModelListener = new ChatModelListener:

    override def onRequest(ctx: ChatModelRequestContext): Unit =
      val params = ctx.chatRequest().parameters()
      startSpan(s"chat ${params.modelName()}", "chat", ctx.modelProvider(), str(params.modelName()), ctx.attributes())
        .setAttribute("gen_ai.request.message_count", ctx.chatRequest().messages().size().toLong)

    override def onResponse(ctx: ChatModelResponseContext): Unit =
      val meta  = ctx.chatResponse().metadata()
      val usage = Option(meta.tokenUsage())
      val attrs = metricAttrs("chat", ctx.modelProvider(), str(ctx.chatRequest().parameters().modelName()), Some(str(meta.modelName())))
      finish(ctx.attributes(), attrs) { span =>
        span.setAttribute("gen_ai.response.model", str(meta.modelName()))
        span.setAttribute("gen_ai.response.finish_reason", str(meta.finishReason()))
        recordTokens(span, attrs, "input", usage.flatMap(u => Option(u.inputTokenCount()).map(_.toInt)))
        recordTokens(span, attrs, "output", usage.flatMap(u => Option(u.outputTokenCount()).map(_.toInt)))
      }

    override def onError(ctx: ChatModelErrorContext): Unit =
      finish(ctx.attributes(), metricAttrs("chat", ctx.modelProvider(), str(ctx.chatRequest().parameters().modelName())))(recordError(_, ctx.error()))

  /** Listener für `EmbeddingModel` - ein Span je Embedding-Aufruf (Frage bzw. Chunk-Batch bei der Ingestion) mit Dauer-Metrik. */
  private def embeddingListener(defaultName: String): EmbeddingModelListener = new EmbeddingModelListener:

    private def modelName(req: EmbeddingRequest, model: EmbeddingModel): String =
      Option(req.parameters()).flatMap(p => Option(p.modelName())).getOrElse(defaultName)

    override def onRequest(ctx: EmbeddingModelRequestContext): Unit =
      val model = modelName(ctx.embeddingRequest(), ctx.embeddingModel())
      startSpan(s"embeddings $model", "embeddings", ctx.modelProvider(), model, ctx.attributes())
        .setAttribute("gen_ai.request.input_count", ctx.embeddingRequest().inputs().size().toLong)

    override def onResponse(ctx: EmbeddingModelResponseContext): Unit =
      val model = modelName(ctx.embeddingRequest(), ctx.embeddingModel())
      finish(ctx.attributes(), metricAttrs("embeddings", ctx.modelProvider(), model, Some(model)))(_ => ())

    override def onError(ctx: EmbeddingModelErrorContext): Unit =
      val model = modelName(ctx.embeddingRequest(), ctx.embeddingModel())
      finish(ctx.attributes(), metricAttrs("embeddings", ctx.modelProvider(), model))(recordError(_, ctx.error()))

  /** Bei ausgeschalteter Observability leer - dann entstehen weder Spans noch Metriken. */
  val chatListeners: JList[ChatModelListener] = if enabled then JList.of(chatListener) else JList.of()

  /** Hängt den Embedding-Listener an ein Modell (das lokale ONNX-Modell hat keinen `listeners(...)`-Builder); bei ausgeschalteter Observability unverändert. */
  def traced(model: EmbeddingModel, name: String): EmbeddingModel = if enabled then model.addListener(embeddingListener(name)) else model

  /** Umhüllt einen `ContentRetriever` mit einem `rag.retrieve`-Span (Embedding der Frage + Vektorsuche). */
  def traced(delegate: ContentRetriever): ContentRetriever = (query: Query) =>
    span("rag.retrieve", "rag.query" -> query.text()) { s =>
      val contents: JList[Content] = delegate.retrieve(query)
      s.setAttribute("rag.results", contents.size().toLong)
      contents
    }
