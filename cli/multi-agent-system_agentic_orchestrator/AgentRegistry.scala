package agents

/** Zentrale Registry aller verfügbaren Agenten. Einziger Ort, der angepasst werden muss, um einen neuen Agenten in das System einzubinden - weder `Orchestrator` (Execution) noch `AgentOrchestrator`
  * (Planning) kennen die konkreten Agenten, sie arbeiten nur generisch mit `AgentSpec`-Werten.
  */
object AgentRegistry:

  def specs(topic: String): List[AgentSpec] = List(
    AgentFactResearcher.spec(topic),
    AgentRiskAnalyst.spec(topic),
    AgentSynthesis.spec(topic),
  )
