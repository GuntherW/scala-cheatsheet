package agents

import kyo.*

/** Generische, LLM-unabhängige Beschreibung eines Agenten - die Grundlage dafür, dass der Orchestrator generisch (erweiterbar) und agentisch (LLM-planbar) wird.
  *
  * Analog zum sttp-ai-Original (`AgentSpec.scala`): beschreibt einen Agenten als "Knoten" in einem Abhängigkeitsgraphen. `execute` ist hier effektvoll (`< (Async & Sync)`, statt eines reinen
  * `String`) - der eigentliche Model-Call (`AI.gen` via `Agents.run`/`Agents.runStructured`) läuft innerhalb dieses Kyo-Effekts. `LLM` UND `Abort[AIGenException]` werden dabei bereits innerhalb von
  * `Agents.run`/`Agents.runStructured` selbst discharged (siehe dort) - ein einzelner fehlgeschlagener Agent soll die gesamte Pipeline nicht abbrechen (`ReportOutcome`/`AgentRunOutcome` statt einer
  * durchgereichten `Abort`), daher taucht hier weder `LLM` noch `Abort[AIGenException]` auf.
  *
  * @param id
  *   eindeutiger Bezeichner, wird sowohl vom generischen Executor als auch vom Planungs-Agent (LLM) referenziert.
  * @param description
  *   Freitext-Beschreibung der Fähigkeit des Agenten - wird dem Planungs-Agent als Kontext mitgegeben.
  * @param hardDependsOn
  *   IDs von Agenten, deren Output dieser Agent zwingend als Kontext benötigt (siehe `PlanValidator`).
  * @param isMandatory
  *   `true`, falls dieser Agent in JEDEM Plan enthalten sein muss (siehe `PlanValidator`).
  * @param execute
  *   führt den Agenten aus; bekommt die Outputs aller bisher gelaufenen Agenten als Kontext-Map.
  */
final case class AgentSpec(
    id: String,
    description: String,
    hardDependsOn: Set[String] = Set.empty,
    isMandatory: Boolean = false,
    execute: Map[String, String] => String < (Async & Sync),
)
