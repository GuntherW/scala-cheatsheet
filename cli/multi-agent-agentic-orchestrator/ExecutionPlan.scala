package agents

import io.circe.Decoder
import sttp.tapir.Schema

/** Ergebnis der Planning-Phase (siehe `AgentOrchestrator`): eine Liste von "Steps". Alle Agenten innerhalb eines Steps sind voneinander unabhängig und werden vom Executor (`Orchestrator`) parallel
  * ausgeführt; ein Step startet erst, nachdem der vorherige vollständig abgeschlossen ist (Fan-out/Fan-in pro Step).
  *
  * @param steps
  *   Level für Level (äußere Liste = Reihenfolge/Synchronisationspunkte, innere Liste = parallel laufende agent-ids).
  * @param finalAgentId
  *   id des Agenten, dessen Output als finales Ergebnis der Pipeline gilt (z. B. der Synthesis-Agent).
  * @param reasoning
  *   kurze Begründung des Planungs-Agent (LLM) für diesen Plan - dient nur der Nachvollziehbarkeit/Logging, hat keine funktionale Bedeutung.
  */
final case class ExecutionPlan(
    steps: List[List[String]],
    finalAgentId: String,
    reasoning: String,
) derives Schema,
      Decoder
