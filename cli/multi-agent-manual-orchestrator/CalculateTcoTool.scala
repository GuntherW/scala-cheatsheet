package agents

import io.circe.{Codec, Json}
import io.circe.syntax.*
import sttp.ai.claude.models.{PropertySchema, Tool, ToolInputSchema}

/** Eingabeparameter, wie sie das Modell (passend zum `inputSchema` von `CalculateTcoTool.definition`) liefert.
  */
case class CalculateTcoInput(technology: String, team_size: Int) derives Codec.AsObject

/** Rückgabe des Tools - bewusst als eigenes Case-Class-Schema, damit die JSON-Struktur klar erkennbar bleibt.
  */
case class CalculateTcoResult(technology: String, team_size: Int, estimated_monthly_cost_eur: Int, note: String) derives Codec.AsObject

/** Definition und Ausführung des client-seitigen (custom) Tools `calculate_tco`, genutzt vom `RiskAnalyst` (siehe `AgentRiskAnalyst.scala`). Eigene Datei, damit die Tool-Definition (JSON-Schema) und
  * -Ausführung (Handler) klar getrennt vom Agenten selbst sichtbar sind.
  */
object CalculateTcoTool:

  /** Client-seitiges (custom) Tool: Definition per JSON-Schema (`ToolInputSchema`/`PropertySchema` aus sttp-ai). Das Modell entscheidet selbst, WANN es dieses Tool mit welchen Parametern aufruft -
    * die eigentliche Ausführung übernimmt `handler` unten.
    */
  val definition: Tool.Custom = Tool(
    name = "calculate_tco",
    description = "Berechnet eine grobe geschätzte Total Cost of Ownership (TCO) pro Monat für " +
      "eine Technologie, basierend auf der Teamgröße. HINWEIS: Dies ist eine Demo-Berechnung mit " +
      "Dummy-Zahlen, keine echte Kostenanalyse.",
    inputSchema = ToolInputSchema.forObject(
      properties = Map(
        "technology" -> PropertySchema.string("Name der zu bewertenden Technologie, z. B. 'Kubernetes'"),
        "team_size"  -> PropertySchema.integer("Anzahl der Teammitglieder, die die Technologie betreiben/nutzen"),
      ),
      required = Some(List("technology", "team_size")),
    ),
  )

  /** Dummy-Implementierung des calculate_tco-Tools.
    *
    * Führt keine echte Kostenanalyse durch, sondern demonstriert nur, wie ein client-seitiges Tool lokal ausgeführt und dessen Ergebnis als JSON-String an das Modell zurückgegeben wird.
    */
  def handler(rawInput: Map[String, Json]): String =
    val input          = Json.fromFields(rawInput).as[CalculateTcoInput]
      .getOrElse(throw new RuntimeException(s"Konnte calculate_tco-Eingabe nicht parsen: $rawInput"))
    // Fest codierte Dummy-Formel - rein illustrativ.
    val monthlyCostEur = 350 * input.team_size + 500
    val result         = CalculateTcoResult(
      technology = input.technology,
      team_size = input.team_size,
      estimated_monthly_cost_eur = monthlyCostEur,
      note = "Demo-Berechnung mit Dummy-Zahlen, keine reale Kostenanalyse.",
    )
    result.asJson.noSpaces
