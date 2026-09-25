package agents

/** Validiert/repariert einen vom `AgentPlanner` (LLM) gelieferten `ExecutionPlan`, bevor der `Orchestrator` ihn ausführt.
  *
  * Notwendig, weil der Plan von einem LLM stammt und daher potenziell fehlerhaft sein kann (unbekannte agent-ids, verletzte Abhängigkeiten, vergessene Pflicht-Agenten, ungültige `finalAgentId`).
  * `validate` liefert IMMER einen strukturell korrekten Plan zurück (nie eine Exception) - im Zweifel unter Verlust der vom LLM vorgeschlagenen Parallel-Gruppierung, aber niemals unter Verletzung der
  * in der `AgentRegistry` deklarierten `hardDependsOn`-Constraints.
  *
  * Reine, LLM-freie Logik - unverändert gegenüber dem sttp-ai-Original (`PlanValidator.scala`), da sie nie mit `AI`/`LLM` in Berührung kommt.
  *
  * Ablauf:
  *   1. Unbekannte/doppelte agent-ids aus dem Plan entfernen.
  *   1. Fehlende Pflicht-Agenten (`isMandatory`) ergänzen.
  *   1. Transitiven Abschluss über `hardDependsOn` bilden: Ein im Plan vorhandener (oder gerade als Pflicht-Agent ergänzter) Agent darf nicht stillschweigend von einem NICHT im Plan enthaltenen
  *      Agenten abhängen - sonst bekäme z. B. `Synthesis-Agent` (`hardDependsOn = {Fact-Researcher, Risk-Analyst}`) einen fehlenden Upstream-Report nie mitgeteilt, obwohl `Fact-Researcher`/
  *      `Risk-Analyst` selbst nicht `isMandatory` sind und das LLM sie laut `AgentPlanner`-System-Prompt bewusst weglassen darf ("Nicht als Pflicht markierte Agenten darfst du weglassen, falls sie
  *      keinen Mehrwert liefern würden") - genau das "Silent Suppression"-Anti-Pattern (CCAF 5.3), das dieses Projekt an anderer Stelle bewusst vermeidet.
  *   1. Die verbleibende (deduplizierte, um fehlende Abhängigkeiten ergänzte) Reihenfolge dient nur noch als "Wunsch-Priorität" für einen stabilen topologischen Sort nach `hardDependsOn`
  *      (Kahn-Algorithmus) - das garantiert dependency-korrekte Levels, unabhängig davon, wie (in)korrekt das LLM ursprünglich gruppiert hatte. Ein Zyklus in `hardDependsOn` (sollte bei einer
  *      sauberen `AgentRegistry` nie vorkommen) wird defensiv aufgebrochen, statt den Executor in eine Endlosschleife laufen zu lassen.
  *   1. `finalAgentId` validieren, sonst auf einen Pflicht-Agenten (oder den letzten Agenten im Plan) zurückfallen.
  */
object PlanValidator:

  def validate(raw: ExecutionPlan, specs: Map[String, AgentSpec]): ExecutionPlan =
    require(specs.nonEmpty, "AgentRegistry darf nicht leer sein.")

    val knownIds      = specs.keySet
    val deduped       = raw.steps.flatten.filter(knownIds.contains).distinct
    val mandatory     = specs.values.filter(_.isMandatory).map(_.id).filterNot(deduped.contains)
    val withMandatory = deduped ++ mandatory
    val orderedIds    = closeOverDependencies(withMandatory, withMandatory, specs, knownIds)

    val levels = topologicalLevels(orderedIds, specs)
    val allIds = levels.flatten

    val finalAgentId =
      if allIds.contains(raw.finalAgentId) then raw.finalAgentId
      else specs.values.find(_.isMandatory).map(_.id).orElse(allIds.lastOption).getOrElse(specs.keys.head)

    ExecutionPlan(levels, finalAgentId, raw.reasoning)

  /** Erweitert `ids` um alle transitiv über `hardDependsOn` benötigten, aber noch nicht enthaltenen Agenten-ids (siehe Klassendoc). Terminiert immer: jede id wird höchstens einmal ergänzt (`knownIds`
    * ist endlich), ein Zyklus in `hardDependsOn` führt daher nie zu einer Endlosschleife.
    */
  @annotation.tailrec
  private def closeOverDependencies(frontier: List[String], acc: List[String], specs: Map[String, AgentSpec], knownIds: Set[String]): List[String] =
    val newDeps = frontier
      .flatMap(id => specs.get(id).map(_.hardDependsOn).getOrElse(Set.empty))
      .filter(knownIds.contains)
      .filterNot(acc.contains)
      .distinct
    if newDeps.isEmpty then acc else closeOverDependencies(newDeps, acc ++ newDeps, specs, knownIds)

  /** Stabiler topologischer Sort (Kahn-Algorithmus) von `ids` nach `hardDependsOn`: Agenten ohne offene Abhängigkeiten bilden ein Level (= parallel ausführbar), sortiert nach ihrer Position in `ids`
    * (Wunsch-Priorität des LLM-Plans als Tie-Breaker). Bricht einen eventuellen Zyklus defensiv auf, indem die verbleibenden (nicht auflösbaren) IDs als letztes Level angehängt werden, statt in eine
    * Endlosschleife zu laufen.
    */
  private def topologicalLevels(ids: List[String], specs: Map[String, AgentSpec]): List[List[String]] =
    val priority = ids.zipWithIndex.toMap

    @annotation.tailrec
    def loop(remaining: List[String], doneIds: Set[String], levelsAcc: List[List[String]]): List[List[String]] =
      if remaining.isEmpty then levelsAcc.reverse
      else
        val (ready, notReady) = remaining.partition(id => specs(id).hardDependsOn.subsetOf(doneIds))
        if ready.isEmpty then
          // Zyklus bzw. Abhängigkeit auf eine nicht in `ids` enthaltene id: defensiv auflösen, damit der Executor nicht blockiert.
          (remaining.sortBy(priority) :: levelsAcc).reverse
        else
          val sortedReady = ready.sortBy(priority)
          loop(notReady, doneIds ++ sortedReady, sortedReady :: levelsAcc)

    loop(ids, Set.empty, Nil)
