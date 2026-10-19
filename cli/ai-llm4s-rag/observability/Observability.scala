package rag.observability

import org.llm4s.llmconnect.config.{LangfuseConfig, TracingSettings}
import org.llm4s.trace.{Tracing, TracingMode}

import com.typesafe.config.{Config, ConfigFactory}

/** Zentrale Verdrahtung der Observability: Traces gehen per OpenTelemetry (OTLP/gRPC) an OpenObserve.
  *
  * {{{
  *   RAG (withTracing) --> OtelSink --> OTLP/gRPC :5081 --> OpenObserve  http://localhost:5080
  *        ^
  *        +-- RagTracing meldet nach der Antwort zusätzlich LLM-Tokens und Gesamtdauer nach
  * }}}
  *
  * Schalter: `rag.observability.enable.otel` (Env `RAG_OBS_OTEL`, Standard an). Ist er aus, läuft ein NoOp-Tracer.
  */
final case class Observability(tracer: Tracing, otelActive: Boolean):

  /** Der OTel-Exporter sendet gebündelt (Batch) - erst `shutdown` sendet den Rest, sonst gehen bei kurzen CLI-Läufen Spans verloren. */
  def shutdown(): Unit = tracer.shutdown()

object Observability:

  def build(config: Config = ConfigFactory.load()): Observability =
    val obs = config.getConfig("rag.observability")

    if obs.getBoolean("enable.otel") then Observability(OtelSink.create(otelSettings(obs.getConfig("otel"))), otelActive = true)
    else Observability(Tracing.create(TracingSettings(TracingMode.NoOp, LangfuseConfig())), otelActive = false)

  private def otelSettings(c: Config): OtelSink.Settings =
    OtelSink.Settings(c.getString("serviceName"), c.getString("endpoint"), c.getString("authorization"), c.getString("organization"), c.getString("stream"))
