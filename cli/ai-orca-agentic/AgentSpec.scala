/** Knoten im Abhängigkeitsgraphen. `execute` bekommt die Outputs aller bisher gelaufenen Agenten.
  *
  * Ein neuer Agent kommt nur in `AgentRegistry` dazu. Planner und Executor kennen die konkreten Agenten nicht.
  */
final case class AgentSpec(
    id: String,
    description: String,
    hardDependsOn: Set[String] = Set.empty,
    isMandatory: Boolean = false,
    execute: Map[String, String] => String,
)
