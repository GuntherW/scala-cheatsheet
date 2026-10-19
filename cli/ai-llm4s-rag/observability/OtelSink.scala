package rag.observability

import org.llm4s.llmconnect.config.{LangfuseConfig, OpenTelemetryConfig, TracingSettings}
import org.llm4s.trace.{Tracing, TracingMode}

/** Ziel: OpenTelemetry (OTLP/gRPC) nach OpenObserve (`docker compose --profile otel up -d openobserve`).
  *
  * Was kommt an: Spans im herstellerneutralen OTel-Format (Service `ai-rag-llm4s`) - jeder OTLP-fähige Dienst
  *               (Jaeger, SigNoz, Tempo, ...) könnte hier stehen, nur Endpoint und Header ändern sich.
  * Wo ansehen:   http://localhost:5080 (Login: OPENOBSERVE_ROOT_EMAIL / _PASSWORD aus docker/.env.observability) -> Traces.
  * Schalter:     `rag.observability.enable.otel` (Env `RAG_OBS_OTEL`, Standard an).
  *
  * OpenObserve erwartet am gRPC-Port (5081) Basic-Auth sowie die Header `organization` und `stream-name`.
  */
object OtelSink:

  final case class Settings(serviceName: String, endpoint: String, authorization: String, organization: String, stream: String)

  def create(settings: Settings): Tracing =
    val headers = Map("authorization" -> settings.authorization, "organization" -> settings.organization, "stream-name" -> settings.stream)
    Tracing.create(
      TracingSettings(TracingMode.OpenTelemetry, LangfuseConfig(), OpenTelemetryConfig(settings.serviceName, settings.endpoint, headers))
    )
