package rag

import rag.observability.Observability
import com.typesafe.config.ConfigFactory
import munit.FunSuite

/** Testet die Sink-Auswahl aus der Konfiguration - ohne Netz, LLM oder DB. */
class ObservabilityTest extends FunSuite:

  private def config(otel: Boolean) =
    ConfigFactory.parseString(s"rag.observability.enable.otel = $otel").withFallback(ConfigFactory.load()).resolve()

  test("otel aus: NoOp-Tracer, nichts aktiv") {
    val obs = Observability.build(config(otel = false))
    assert(!obs.otelActive)
    assert(obs.tracer.traceEvent("noop").isRight)
  }

  test("otel an: OTel-Tracer ist aktiv") {
    val obs = Observability.build(config(otel = true))
    assert(obs.otelActive)
    obs.shutdown()
  }
