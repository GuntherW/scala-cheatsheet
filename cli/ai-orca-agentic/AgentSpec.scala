/** Reine Planungs-Metadaten für einen Knoten im Abhängigkeitsgraphen - keine Ausführungslogik, kein `orca`-Import nötig. Das hält `PlanValidator` (und seine Tests) unabhängig vom Orca-Backend
  * testbar.
  *
  * Die tatsächliche Ausführung (`Map[String, String] => String`, die Outputs aller bisher gelaufenen Agenten entgegennimmt) liefert `AgentRegistry.bind` separat - sie wird nur zur Laufzeit innerhalb
  * einer Stage gebraucht, nicht für Planung/Validierung.
  *
  * Ein neuer Agent kommt nur in `AgentRegistry` dazu. Planner und Executor kennen die konkreten Agenten nicht.
  */
final case class AgentSpec(
    id: String,
    description: String,
    hardDependsOn: Set[String] = Set.empty,
    isMandatory: Boolean = false,
)
