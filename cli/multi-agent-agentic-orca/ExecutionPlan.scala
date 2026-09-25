/** Vom Planner-LLM geliefert, vom `PlanValidator` repariert.
  *
  * Äußere Liste = Reihenfolge. Innere Liste = Agenten, die parallel laufen dürfen.
  */
final case class ExecutionPlan(
    steps: List[List[String]],
    finalAgentId: String,
    reasoning: String,
)
