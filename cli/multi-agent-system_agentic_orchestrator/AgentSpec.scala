package agents

/** Generische, LLM-unabhängige Beschreibung eines Agenten - die Grundlage dafür, dass der Orchestrator generisch (erweiterbar) und agentisch (LLM-planbar) wird.
  *
  * Während `Agent` (siehe `Agent.scala`) den eigentlichen Model-Call kapselt, beschreibt `AgentSpec` einen Agenten als "Knoten" in einem Abhängigkeitsgraphen: welche ID hat er, wovon hängt er ab, und
  * wie wird er generisch ausgeführt (`Map[String, String] => String`, wobei die Map die Outputs seiner Abhängigkeiten enthält, aufgeschlüsselt nach deren `id`).
  *
  * Ein neuer Agent wird eingebunden, indem lediglich eine neue `AgentSpec`-Instanz der `AgentRegistry` hinzugefügt wird - weder der `Orchestrator` (Execution-Phase) noch der `OrchestratorAgent`
  * (Planning-Phase) müssen dafür angepasst werden.
  *
  * @param id
  *   eindeutiger Bezeichner, wird sowohl vom generischen Executor als auch vom Planungs-Agent (LLM) referenziert.
  * @param description
  *   Freitext-Beschreibung der Fähigkeit des Agenten - wird dem Planungs-Agent als Kontext mitgegeben, damit dieser sinnvoll entscheiden kann, wann/ob dieser Agent aufgerufen werden soll.
  * @param hardDependsOn
  *   IDs von Agenten, deren Output dieser Agent zwingend als Kontext benötigt. Der Planungs-Agent MUSS diese Constraint einhalten (wird von `PlanValidator` erzwungen/repariert, falls das LLM sie
  *   verletzt).
  * @param isMandatory
  *   `true`, falls dieser Agent in JEDEM Plan enthalten sein muss (z. B. ein Aggregator/Synthesis-Agent, ohne den kein sinnvolles Endergebnis entsteht). `PlanValidator` ergänzt fehlende
  *   Pflicht-Agenten automatisch.
  * @param execute
  *   führt den Agenten aus; bekommt die Outputs aller bisher gelaufenen Agenten (nicht nur der eigenen Abhängigkeiten) als Kontext-Map, filtert sich das intern selbst heraus, was er braucht.
  */
final case class AgentSpec(
    id: String,
    description: String,
    hardDependsOn: Set[String] = Set.empty,
    isMandatory: Boolean = false,
    execute: Map[String, String] => String,
)
