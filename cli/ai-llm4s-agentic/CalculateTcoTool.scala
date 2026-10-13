package agents

import org.llm4s.toolapi.{SafeParameterExtractor, Schema, ToolBuilder, ToolFunction}
import upickle.default.{macroRW, ReadWriter}

case class CalculateTcoResult(technology: String, teamSize: Int, estimatedMonthlyCostEur: Int, note: String)

object CalculateTcoResult:
  given ReadWriter[CalculateTcoResult] = macroRW

/** Client-seitiges (custom) Tool `calculate_tco`: Definition per `Schema` (JSON-Schema) + lokaler Handler. llm4s ruft den Handler automatisch auf, wenn das Modell das Tool anfordert. */
object CalculateTcoTool:

  val tool: ToolFunction[Map[String, Any], CalculateTcoResult] =
    ToolBuilder[Map[String, Any], CalculateTcoResult](
      name = "calculate_tco",
      description = "Berechnet eine grobe geschätzte Total Cost of Ownership (TCO) pro Monat für eine Technologie, basierend auf der Teamgröße. HINWEIS: Demo-Berechnung mit Dummy-Zahlen.",
      schema = Schema
        .`object`[Map[String, Any]]("TCO-Parameter")
        .withRequiredField("technology", Schema.string("Name der zu bewertenden Technologie, z. B. 'Kubernetes'"))
        .withRequiredField("team_size", Schema.integer("Anzahl der Teammitglieder, die die Technologie betreiben/nutzen")),
    ).withHandler(handler)
      .buildSafe()
      .fold(error => throw new RuntimeException(s"calculate_tco konnte nicht gebaut werden: ${error.formatted}"), identity)

  /** Ein `Left` wird dem Modell als Tool-Fehler gezeigt (nicht als Exception geworfen) - das Modell kann dann selbst reagieren. */
  private def handler(params: SafeParameterExtractor): Either[String, CalculateTcoResult] =
    for
      technology <- params.getString("technology")
      teamSize   <- params.getInt("team_size")
    yield CalculateTcoResult(
      technology = technology,
      teamSize = teamSize,
      estimatedMonthlyCostEur = 350 * teamSize + 500,
      note = "Demo-Berechnung mit Dummy-Zahlen, keine reale Kostenanalyse.",
    )
