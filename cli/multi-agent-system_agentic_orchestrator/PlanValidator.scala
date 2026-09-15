package agents

import scala.collection.mutable.ListBuffer

/** Validiert/repariert einen vom `AgentOrchestrator` (LLM) gelieferten `ExecutionPlan`, bevor der `Orchestrator` ihn ausführt.
  *
  * Notwendig, weil der Plan von einem LLM stammt und daher potenziell fehlerhaft sein kann (unbekannte agent-ids, verletzte Abhängigkeiten, vergessene Pflicht-Agenten, ungültige `finalAgentId`).
  * `validate` liefert IMMER einen strukturell korrekten Plan zurück (nie eine Exception) - im Zweifel unter Verlust der vom LLM vorgeschlagenen Parallel-Gruppierung, aber niemals unter Verletzung der
  * in der `AgentRegistry` deklarierten `hardDependsOn`-Constraints.
  *
  * Ablauf:
  *   1. Unbekannte/doppelte agent-ids aus dem Plan entfernen.
  *   1. Fehlende Pflicht-Agenten (`isMandatory`) ergänzen.
  *   1. Die verbleibende (deduplizierte) Reihenfolge dient nur noch als "Wunsch-Priorität" für einen stabilen topologischen Sort nach `hardDependsOn` (Kahn-Algorithmus) - das garantiert
  *      dependency-korrekte Levels, unabhängig davon, wie (in)korrekt das LLM ursprünglich gruppiert hatte. Ein Zyklus in `hardDependsOn` (sollte bei einer sauberen `AgentRegistry` nie vorkommen)
  *      wird defensiv aufgebrochen, statt den Executor in eine Endlosschleife laufen zu lassen.
  *   1. `finalAgentId` validieren, sonst auf einen Pflicht-Agenten (oder den letzten Agenten im Plan) zurückfallen.
  */
object PlanValidator:

  def validate(raw: ExecutionPlan, specs: Map[String, AgentSpec]): ExecutionPlan =
    require(specs.nonEmpty, "AgentRegistry darf nicht leer sein.")

    val knownIds   = specs.keySet
    val deduped    = raw.steps.flatten.filter(knownIds.contains).distinct
    val mandatory  = specs.values.filter(_.isMandatory).map(_.id).filterNot(deduped.contains)
    val orderedIds = deduped ++ mandatory

    val levels = topologicalLevels(orderedIds, specs)

    val finalAgentId =
      if levels.flatten.contains(raw.finalAgentId) then raw.finalAgentId
      else specs.values.find(_.isMandatory).map(_.id).orElse(levels.flatten.lastOption).getOrElse(specs.keys.head)

    ExecutionPlan(levels, finalAgentId, raw.reasoning)

  /** Stabiler topologischer Sort (Kahn-Algorithmus) von `ids` nach `hardDependsOn`: Agenten ohne offene Abhängigkeiten bilden ein Level (= parallel ausführbar), sortiert nach ihrer Position in `ids`
    * (Wunsch-Priorität des LLM-Plans als Tie-Breaker). Bricht einen eventuellen Zyklus defensiv auf, indem die verbleibenden (nicht auflösbaren) IDs als letztes Level angehängt werden, statt in eine
    * Endlosschleife zu laufen.
    */
  private def topologicalLevels(ids: List[String], specs: Map[String, AgentSpec]): List[List[String]] =
    val priority  = ids.zipWithIndex.toMap
    val levels    = ListBuffer.empty[List[String]]
    var doneIds   = Set.empty[String]
    var remaining = ids

    while remaining.nonEmpty do
      val (ready, notReady) = remaining.partition(id => specs(id).hardDependsOn.subsetOf(doneIds))
      if ready.isEmpty then
        // Zyklus bzw. Abhängigkeit auf eine nicht in `ids` enthaltene id: defensiv auflösen, damit der Executor nicht blockiert.
        levels += remaining.sortBy(priority)
        remaining = Nil
      else
        val sortedReady = ready.sortBy(priority)
        levels += sortedReady
        doneIds ++= sortedReady.toSet
        remaining = notReady
    end while

    levels.toList
