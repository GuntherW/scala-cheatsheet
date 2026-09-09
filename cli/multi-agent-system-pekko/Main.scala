package agents

import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Behavior, Props, SpawnProtocol}
import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
import org.apache.pekko.util.Timeout

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.*

/** Zeitmessung eines Pipeline-Durchlaufs, analog zu `Orchestrator.Timing` in `research_scala`.
  */
final case class Timing(workersSeconds: Double, totalSeconds: Double)

/** Gesamtergebnis eines Pipeline-Durchlaufs, analog zu `Orchestrator.PipelineResult` in `research_scala`.
  */
final case class PipelineResult(
    topic: String,
    factResearcherOutput: String,
    riskAnalystOutput: String,
    finalReport: String,
    timing: Timing,
)

/** Einstiegspunkt UND Orchestrierung in einem: Startet ein `ActorSystem`, spawnt die drei Agenten-Aktoren und koordiniert den Ablauf (Fan-out/ Fan-in) direkt über normale `Future`-Komposition (`ask`,
  * `zip`, `for`-Comprehension) - ohne einen eigenen Orchestrator-Aktor.
  *
  * '''Warum kein eigener Orchestrator-Aktor?''' Die Koordinationslogik hier ("frage zwei Agenten parallel, warte auf beide, frage dann einen dritten") ist ein einmaliger, linearer Ablauf ohne eigenen
  * Zustand, der über mehrere Nachrichten hinweg gepflegt werden müsste. Für genau so einen Fall bietet Scala mit `Future`-Kombinatoren (`zip`, `flatMap`, `for`-Comprehension) bereits ein simples,
  * deklaratives Werkzeug - einen zusätzlichen Aktor NUR für die Koordination zu bauen, würde hier nur zusätzliche Indirektion ohne Mehrwert erzeugen. Aktoren lohnen sich, wenn eine Einheit über die
  * Zeit Zustand oder eigene Nachrichten verwalten muss (wie die Agenten selbst: sie warten auf Model-Antworten und laufen bei Bedarf Tool-Use-Loops) - reine Verkettung von Anfragen tut das nicht.
  *
  * '''`SpawnProtocol`''': Ein von Pekko fertig mitgeliefertes "Guardian"-Verhalten, mit dem sich von AUSSERHALB der Aktoren-Welt (hier: `@main`) neue Aktoren erzeugen und ihre `ActorRef` per `ask`
  * zurückholen lassen (`system.ask(SpawnProtocol.Spawn(...))`). Das erspart das Schreiben eines eigenen Guardian-Aktors nur zum Erzeugen der drei Agenten.
  *
  * Aufruf:
  * {{{
  *   scala-cli run . -- "Sollten wir Kubernetes für unser 5-Personen-Startup einführen?"
  * }}}
  *
  * Ohne Argument wird ein Standard-Thema verwendet.
  */
@main def main(args: String*): Unit =
  val defaultTopic = "Sollten wir für unser Backend von REST auf GraphQL wechseln?"
  val topic        = args.headOption.getOrElse(defaultTopic)

  val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "research-agent-system")
  given ActorSystem[?]                           = system
  given ExecutionContext                         = system.executionContext
  given Timeout                                  = Timeout(5.minutes)

  /** Spawnt einen Aktor unter dem `SpawnProtocol`-Guardian und liefert dessen `ActorRef` als `Future` zurück.
    */
  def spawn[T](behavior: Behavior[T], name: String): Future[ActorRef[T]] =
    system.ask(replyTo => SpawnProtocol.Spawn(behavior, name, Props.empty, replyTo))

  try
    val pipeline: Future[PipelineResult] =
      for
        factResearcher <- spawn(FactResearcherActor(), "fact-researcher")
        riskAnalyst    <- spawn(RiskAnalystActor(), "risk-analyst")
        synthesisAgent <- spawn(SynthesisAgentActor(), "synthesis-agent")
        start           = System.nanoTime()
        _               = println(s"[Pipeline] Fan-out: frage Fact-Researcher und Risk-Analyst PARALLEL für: '$topic'")

        // Fan-out: BEIDE `ask`-Aufrufe starten sofort nacheinander, ohne
        // dass hier bereits auf eine Antwort gewartet wird - die beiden
        // zurückgelieferten `Future`s laufen daher nebenläufig.
        factsFuture: Future[AgentProtocol.Result] = factResearcher.ask(replyTo => AgentProtocol.Run(FactResearcherActor.prompt(topic), maxTokens = 2000, replyTo))
        risksFuture: Future[AgentProtocol.Result] = riskAnalyst.ask(replyTo => AgentProtocol.Run(RiskAnalystActor.prompt(topic), maxTokens = 2000, replyTo))

        // Fan-in: `zip` liefert erst, wenn BEIDE Futures fertig sind.
        (facts, risks) <- factsFuture.zip(risksFuture)
        workersElapsed  = (System.nanoTime() - start) / 1e9
        _               = println(f"[Pipeline] Beide Worker fertig nach $workersElapsed%.1fs - starte Synthesis-Agent")
        report         <- synthesisAgent.ask((replyTo: ActorRef[AgentProtocol.Result]) =>
                            AgentProtocol.Run(SynthesisAgentActor.prompt(topic, facts.output, risks.output), maxTokens = 3000, replyTo)
                          )
        totalElapsed    = (System.nanoTime() - start) / 1e9
        _               = println(f"[Pipeline] Fertig nach insgesamt $totalElapsed%.1fs")
      yield PipelineResult(
        topic = topic,
        factResearcherOutput = facts.output,
        riskAnalystOutput = risks.output,
        finalReport = report.output,
        timing = Timing(workersElapsed, totalElapsed),
      )

    val result = Await.result(pipeline, 5.minutes)

    val outputDir = os.pwd / "output"
    os.makeDir.all(outputDir)

    os.write.over(
      outputDir / "01_fact_researcher.md",
      s"# Fact-Researcher: $topic\n\n${result.factResearcherOutput}\n",
    )
    os.write.over(
      outputDir / "02_risk_analyst.md",
      s"# Risk-Analyst: $topic\n\n${result.riskAnalystOutput}\n",
    )
    os.write.over(
      outputDir / "03_final_report.md",
      s"# Finaler Bericht: $topic\n\n${result.finalReport}\n",
    )

    println("\n=== FINALER BERICHT ===\n")
    println(result.finalReport)
    println(s"\n[Ergebnisse gespeichert in: $outputDir]")
  finally
    AnthropicClient.close()
    system.terminate()
