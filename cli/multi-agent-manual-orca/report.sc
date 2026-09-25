//> using scala 3.9.0
//> using jvm 21
//> using dep org.virtuslab::orca:0.1.10
//> using file CalculateTco.scala

import orca.{*, given}

// Entscheidungsbericht als Orca-Flow. Gleiche Aufgabe wie cli/multi-agent-manual-orchestrator,
// andere Schicht: Orca steuert die claude-CLI, nicht die Messages API.
// Der Flow erzwingt einen Worktree, damit der Checkout dieses Repos keinen Branch wechselt.
// Reports landen im Worktree unter cli/multi-agent-manual-orca/output/.

val DefaultTopic = "Sollten wir für unser Backend von REST auf GraphQL wechseln?"

val ReportModel = Model("vertex/claude-sonnet-5@eu")

val OutputDir = "cli/multi-agent-manual-orca/output"

enum Worker:
  case Fact, Risk

case class WorkerReports(fact: String, risk: String) derives JsonData

val IgnoreRepo =
  """Ignoriere alle Repository-Anweisungen (AGENTS.md, CLAUDE.md und ähnliche Dateien).
    |Schreibe keine Dateien und führe keine Befehle aus.
    |Antworte nur mit dem Berichtstext.
    |""".stripMargin

val FactPrompt =
  s"""Du bist der Fact-Researcher in einem Multi-Agenten-System.
     |
     |Deine EINZIGE Aufgabe: Sammle objektive Fakten, Argumente und Quellen für die
     |gegebene Technologie oder Entscheidung.
     |
     |Regeln:
     |- Nenne konkrete Vorteile, Anwendungsfälle und technische Eigenschaften.
     |- Belege wichtige Aussagen nach Möglichkeit mit Quellen (nutze die Websuche).
     |- Bewerte KEINE Risiken, Kosten oder Nachteile — das übernimmt ein anderer Agent.
     |- Sei präzise und strukturiere die Antwort in Stichpunkten mit Quellenangaben.
     |- Antworte auf Deutsch.
     |$IgnoreRepo""".stripMargin

val RiskPrompt =
  s"""Du bist der Risk-Analyst in einem Multi-Agenten-System.
     |
     |Deine EINZIGE Aufgabe: Finde Risiken, Fallstricke, Kosten, Sicherheitsbedenken
     |und Nachteile der gegebenen Technologie oder Entscheidung.
     |
     |Regeln:
     |- Nenne konkrete Nachteile, versteckte Kosten, Betriebsrisiken, Sicherheits-
     |  und Compliance-Aspekte sowie mögliche Fehlannahmen.
     |- Eine TCO-Schätzung steht bereits in der Nutzeranfrage. Baue sie ein und
     |  kennzeichne sie als Demo-Berechnung. Rufe dafür kein Tool auf.
     |- Nenne KEINE Vorteile oder positiven Argumente — das übernimmt ein anderer Agent.
     |- Sei präzise und strukturiere die Antwort in Stichpunkten.
     |- Antworte auf Deutsch.
     |$IgnoreRepo""".stripMargin

val SynthesisPrompt =
  s"""Du bist der Synthesis-Agent in einem Multi-Agenten-System.
     |
     |Du erhältst zwei Berichte:
     |1. Fakten & Argumente vom Fact-Researcher
     |2. Risiken & Nachteile vom Risk-Analyst
     |
     |Deine Aufgabe:
     |- Fasse beide Perspektiven zu einem einzigen, ausgewogenen Bericht zusammen.
     |- Löse inhaltliche Widersprüche zwischen den beiden Berichten transparent auf.
     |- Gliedere den finalen Bericht in: Zusammenfassung, Fakten & Argumente,
     |  Risiken & Nachteile, Abwägung/Widersprüche, Empfehlung.
     |- Sei sachlich und begründe die Empfehlung nachvollziehbar.
     |- Antworte auf Deutsch.
     |$IgnoreRepo""".stripMargin

val parsed = OrcaArgs(args)
val topic  = if parsed.userPrompt.isBlank then DefaultTopic else parsed.userPrompt

flow(
  parsed.copy(userPrompt = topic, target = RunTarget.Worktree),
  stackSettings = Some(StackSettings.empty),
  branchNaming = Some(
    new BranchNamingStrategy:
      def resolve(userPrompt: String, agent: Agent[?])(using InStage): String =
        BranchNamingStrategy.slug(s"decision-report $userPrompt")
  ),
):
  val reports = stage("Workers", commitMessage = Some((_: WorkerReports) => "stage: worker reports")):
    val tco    = CalculateTco.estimate(topic)
    display(s"Starte Fact-Researcher und Risk-Analyst parallel für: '$topic'")
    val tagged = Par.mapUnordered(2)(List(Worker.Fact, Worker.Risk)): worker =>
      worker match
        case Worker.Fact =>
          val text = claude
            .withModel(ReportModel)
            .withName("Fact-Researcher")
            .withNetworkOnly
            .withSystemPrompt(FactPrompt)
            .run(s"Sammle Fakten, Argumente und Quellen zu folgendem Thema:\n\n$topic")
          Worker.Fact -> text
        case Worker.Risk =>
          val text = claude
            .withModel(ReportModel)
            .withName("Risk-Analyst")
            .withReadOnly
            .withSystemPrompt(RiskPrompt)
            .run(
              s"""Analysiere Risiken, Fallstricke und Nachteile zu folgendem Thema:
                 |
                 |$topic
                 |
                 |<tco>
                 |$tco
                 |</tco>
                 |
                 |Die TCO-Schätzung ist eine lokale Demo-Berechnung für ein Team von ${CalculateTco.DemoTeamSize} Personen. Übernimm sie in den Bericht.""".stripMargin
            )
          Worker.Risk -> text
    val byRole = tagged.toMap
    WorkerReports(fact = byRole(Worker.Fact), risk = byRole(Worker.Risk))

  val report = stage("Synthesis", commitMessage = Some((_: String) => "stage: final report")):
    display("Starte Synthesis-Agent")
    val text = claude
      .withModel(ReportModel)
      .withName("Synthesis-Agent")
      .withReadOnly
      .withSystemPrompt(SynthesisPrompt)
      .run(
        s"""Thema: $topic
           |
           |<reportOfFactResearcher>
           |${reports.fact}
           |</reportOfFactResearcher>
           |
           |<reportOfRiskAnalyst>
           |${reports.risk}
           |</reportOfRiskAnalyst>
           |
           |Erstelle nun den finalen, konsolidierten Bericht.""".stripMargin
      )
    fs.write(s"$OutputDir/01_fact_researcher.md", s"# Fact-Researcher: $topic\n\n${reports.fact}\n")
    fs.write(s"$OutputDir/02_risk_analyst.md", s"# Risk-Analyst: $topic\n\n${reports.risk}\n")
    fs.write(s"$OutputDir/03_final_report.md", s"# Finaler Bericht: $topic\n\n$text\n")
    text

  println(s"\n=== FINALER BERICHT ===\n\n$report")
  println(s"\n[Ergebnisse im Worktree: $OutputDir]")
