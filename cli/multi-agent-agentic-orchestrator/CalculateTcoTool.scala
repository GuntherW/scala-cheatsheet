package agents

import io.circe.Json
import io.circe.derivation.{Configuration, ConfiguredCodec}
import io.circe.syntax.*
import sttp.ai.claude.models.{PropertySchema, Tool, ToolInputSchema}
import sttp.ai.core.agent.AgentTool
import sttp.apispec.{Schema as ApiSchema, SchemaType}
import sttp.shared.Identity

import scala.collection.immutable.ListMap

/** Definition und Ausführung des client-seitigen (custom) Tools `calculate_tco`, genutzt vom `RiskAnalyst` (siehe `AgentRiskAnalyst.scala`). Eigene Datei, damit die Tool-Definition (JSON-Schema) und
  * -Ausführung (Handler) klar getrennt vom Agenten selbst sichtbar sind.
  */
object CalculateTcoTool:

  /** JSON-Feldnamen von `CalculateTcoInput`/`CalculateTcoResult` sollen dem `snake_case` der Anthropic-Tool-Schemas (`team_size`, ...) entsprechen, während die Scala-Felder selbst der
    * Projekt-Konvention `camelCase` folgen (siehe AGENTS.md) - `ConfiguredCodec` (statt schlichtem `Codec.AsObject`) respektiert dafür ein implizites `Configuration`.
    *
    * '''Bewusst als Member DIESES Objekts statt top-level im Package''' (anders als in einer früheren Version dieser Datei): Ein top-level `given` in Scala 3 ist package-weit sichtbar, nicht nur
    * dateilokal - es hätte sonst auch die circe-`derives Codec`-Ableitung von `ExecutionPlan.scala` (ein anderer Typ im selben Package `agents`) auf snake_case umgestellt und dort zu Decoding-Fehlern
    * geführt (`final_agent_id` statt `finalAgentId` erwartet), obwohl `ExecutionPlan` mit dieser Konfiguration nichts zu tun hat.
    */
  private given Configuration = Configuration.default.withSnakeCaseMemberNames

  /** Eingabeparameter, wie sie das Modell (passend zum `inputSchema` von `CalculateTcoTool.definition`) liefert.
    */
  private case class CalculateTcoInput(technology: String, teamSize: Int) derives ConfiguredCodec

  /** Rückgabe des Tools - bewusst als eigenes Case-Class-Schema, damit die JSON-Struktur klar erkennbar bleibt.
    */
  private case class CalculateTcoResult(technology: String, teamSize: Int, estimatedMonthlyCostEur: Int, note: String) derives ConfiguredCodec

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
    * Führt keine echte Kostenanalyse durch, sondern demonstriert nur, wie ein client-seitiges Tool lokal ausgeführt und dessen Ergebnis als JSON-String an das Modell zurückgegeben wird. Ein
    * fehlerhafter Input (z. B. weil das Modell ein Feld weggelassen hat) bricht den Tool-Use-Loop NICHT mit einer Exception ab, sondern wird als JSON-Fehlerobjekt an das Modell zurückgemeldet - so
    * kann das Modell im nächsten Turn selbst reagieren (z. B. das Tool erneut mit korrigierten Parametern aufrufen).
    */
  def handler(rawInput: Map[String, Json]): String =
    Json.fromFields(rawInput).as[CalculateTcoInput] match
      case Left(error)  => Json.obj("error" -> s"Konnte calculate_tco-Eingabe nicht parsen: ${error.getMessage}".asJson).noSpaces
      case Right(input) =>
        // Fest codierte Dummy-Formel - rein illustrativ.
        val monthlyCostEur = 350 * input.teamSize + 500
        CalculateTcoResult(
          technology = input.technology,
          teamSize = input.teamSize,
          estimatedMonthlyCostEur = monthlyCostEur,
          note = "Demo-Berechnung mit Dummy-Zahlen, keine reale Kostenanalyse.",
        ).asJson.noSpaces

  /** Gleiche Definition/Ausführung wie `definition`/`handler` oben, aber als `sttp.ai.core.agent.AgentTool` - der generischen Tool- Abstraktion, die der Interceptor-fähige Agent-Loop
    * (`sttp.ai.core.agent.LoopAgent`, via `sttp.ai.claude.agent.ClaudeAgent`/`AnthropicClient.buildAgent`) erwartet. Bewusst über `AgentTool.dynamic` (rohes `Map[String, Json]`-Input, kein
    * `derives`-Codec für `CalculateTcoInput`) definiert, damit exakt dasselbe, bereits robuste `handler` (inkl. Fehlerbehandlung bei fehlerhaftem Modell-Input) unverändert weiterverwendet werden kann -
    * eine `AgentTool.fromFunction[CalculateTcoInput]` bräuchte zusätzlich eine snake_case-bewusste Tapir-`Schema`-Ableitung für `team_size`, was hier keinen Mehrwert brächte.
    */
  val agentTool: AgentTool[Identity, Map[String, Json]] = AgentTool.dynamic(
    toolName = definition.name,
    toolDescription = definition.description,
    toolSchema = ApiSchema(
      `type` = Some(List(SchemaType.Object)),
      properties = ListMap(
        "technology" -> ApiSchema(`type` = Some(List(SchemaType.String)), description = Some("Name der zu bewertenden Technologie, z. B. 'Kubernetes'")),
        "team_size"  -> ApiSchema(`type` = Some(List(SchemaType.Integer)), description = Some("Anzahl der Teammitglieder, die die Technologie betreiben/nutzen")),
      ),
      required = List("technology", "team_size"),
    ),
  )(handler)
