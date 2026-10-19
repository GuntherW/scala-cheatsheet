package rag.observability

import org.llm4s.llmconnect.config.OpenTelemetryConfig
import org.llm4s.trace.{OpenTelemetryTracing, Tracing}

/** Ziel: OpenTelemetry (OTLP/gRPC) nach OpenObserve (`docker compose --profile otel up -d openobserve`).
  *
  * Was kommt an: Spans im herstellerneutralen OTel-Format (Service `ai-rag-llm4s`) - jeder OTLP-fähige Dienst
  *               (Jaeger, SigNoz, Tempo, ...) könnte hier stehen, nur Endpoint und Header ändern sich.
  * Wo ansehen:   http://localhost:5080 (Login: OPENOBSERVE_ROOT_EMAIL / _PASSWORD aus docker/.env.observability) -> Traces.
  * Schalter:     `rag.observability.enable.otel` (Env `RAG_OBS_OTEL`, Standard an).
  *
  * OpenObserve erwartet am gRPC-Port (5081) Basic-Auth sowie die Header `organization` und `stream-name`.
  *
  * llm4s 0.5.1: Der OTel-Backend liegt in `llm4s-observability-otel`; der Tracer wird direkt über
  * `OpenTelemetryTracing.from(OpenTelemetryConfig(...))` gebaut (statt über das entfernte `TracingMode.OpenTelemetry`).
  */
object OtelSink:

  final case class Settings(serviceName: String, endpoint: String, authorization: String, organization: String, stream: String)

  def create(settings: Settings): Tracing =
    val headers = Map("authorization" -> settings.authorization, "organization" -> settings.organization, "stream-name" -> settings.stream)
    OpenTelemetryTracing.from(OpenTelemetryConfig(settings.serviceName, settings.endpoint, headers))
