package agents

import kyo.*

/** Definition und Ausführung des client-seitigen (custom) Tools `calculate_tco`, genutzt vom `RiskAnalyst` (siehe `AgentRiskAnalyst.scala`). Eigene Datei, damit die Tool-Definition und -Ausführung
  * klar getrennt vom Agenten selbst sichtbar sind.
  *
  * Deutlich einfacher als im sttp-ai-Original (`CalculateTcoTool.scala`): `Tool.init[In]` leitet Eingabe-/Ausgabe-Schema direkt aus `CalculateTcoInput derives Schema`/`CalculateTcoResult derives
  * Schema` ab - kein manuelles JSON-Schema (`ToolInputSchema`/`PropertySchema`), kein rohes `Map[String, Json]`-Parsing und keine manuelle Fehlerrückmeldung an das Modell mehr nötig: kyo-ais
  * Eval-Loop dekodiert die Modell-Argumente selbst und schickt bei einem Decode-Fehler automatisch eine korrigierende System-Message ans Modell zurück (siehe kyo-ai-README, Abschnitt "Tools and the
  * automatic loop").
  */
object CalculateTcoTool:

  private case class CalculateTcoInput(technology: String, teamSize: Int) derives Schema

  private case class CalculateTcoResult(technology: String, teamSize: Int, estimatedMonthlyCostEur: Int, note: String) derives Schema

  /** Dummy-Implementierung: keine echte Kostenanalyse, sondern eine fest codierte Formel, rein illustrativ (wie im Original). */
  val tool: Tool[Any] = Tool.init[CalculateTcoInput](
    "calculate_tco",
    "Berechnet eine grobe geschätzte Total Cost of Ownership (TCO) pro Monat für eine Technologie, " +
      "basierend auf der Teamgröße. HINWEIS: Dies ist eine Demo-Berechnung mit Dummy-Zahlen, keine echte Kostenanalyse.",
  ) { input =>
    val monthlyCostEur = 350 * input.teamSize + 500
    CalculateTcoResult(
      technology = input.technology,
      teamSize = input.teamSize,
      estimatedMonthlyCostEur = monthlyCostEur,
      note = "Demo-Berechnung mit Dummy-Zahlen, keine reale Kostenanalyse.",
    )
  }
