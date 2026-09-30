package agent

import io.circe.Json
import sttp.ai.claude.models.Tool

/** Zentrale Registry aller "echten" (client-seitigen) Tools des Agenten.
  *
  * Im Gegensatz zu `ai-sttpai-manual` (dort werden ALLE Tool-Definitionen bei JEDEM Request ans Modell mitgeschickt) sieht das Modell hier anfangs NUR das Meta-Tool `search_tools` (siehe
  * `SearchToolsTool.scala`). Erst wenn es darüber ein passendes Tool findet, wird dessen echtes JSON-Schema (`ToolMeta.definition`) für die folgenden Turns freigeschaltet (siehe
  * `AnthropicClient.chat`). Das ist das "Tool-Search"- bzw. "Progressive Tool Disclosure"-Paradigma: Bei vielen/großen Tool-Inventaren (in echten Systemen ggf. hunderte Tools) spart man sich so, bei
  * jedem Request alle Schemas mitzuschicken und damit unnötig Context-Budget zu verbrauchen.
  */
object ToolRegistry:

  /** Metadaten zu einem registrierten Tool: Name/Description kommen ohnehin aus der `definition`, `keywords` sind zusätzliche Suchbegriffe (auch deutsche Synonyme), damit `search_tools` das Tool auch
    * bei nicht wortgleichen Anfragen findet (z. B. "wie spät" -> `get_current_time`).
    */
  final case class ToolMeta(
      definition: Tool.Custom,
      keywords: List[String],
      handler: Map[String, Json] => String,
  )

  val all: List[ToolMeta] = List(
    ToolMeta(
      definition = CurrentTimeTool.definition,
      keywords = List("zeit", "uhrzeit", "datum", "time", "date", "clock", "wie spät", "timezone", "zeitzone"),
      handler = CurrentTimeTool.handler,
    ),
    ToolMeta(
      definition = CalculatorTool.definition,
      keywords = List("rechnen", "berechne", "berechnung", "arithmetik", "math", "calculate", "plus", "minus", "mal", "geteilt", "summe", "addieren"),
      handler = CalculatorTool.handler,
    ),
    ToolMeta(
      definition = RollDiceTool.definition,
      keywords = List("würfel", "wuerfel", "würfeln", "dice", "roll", "zufall", "random", "zufallszahl"),
      handler = RollDiceTool.handler,
    ),
  )

  private val byName: Map[String, ToolMeta] = all.map(t => t.definition.name -> t).toMap

  def find(name: String): Option[ToolMeta] = byName.get(name)

  /** Simple Keyword-/Substring-Suche über Name, Description und `keywords` aller registrierten Tools (case-insensitive, jedes Wort der Query wird einzeln gegen jedes Tool geprüft). Bewusst simpel
    * gehalten - in einem echten System stünde hier z. B. eine Vektor-/Embedding-Suche über eine (ggf. sehr große) Tool-Bibliothek.
    *
    * WICHTIG: `\W` ist in Java/Scala standardmäßig NUR ASCII-basiert - Umlaute wie 'ü' gelten dann fälschlich als Trenner ("Würfel" -> "w", "rfel"). Das `(?U)`-Flag schaltet Unicode-bewusstes
    * Wort-Matching ein, damit "würfel" als EIN Token erhalten bleibt. Zusätzlich werden Terme mit weniger als 3 Zeichen verworfen, da sie sonst per Substring-Suche fast überall zufällig "treffen" (z.
    * B. ein einzelnes "w") und damit ungewollt zusätzliche, eigentlich irrelevante Tools freischalten würden.
    */
  def search(query: String): List[ToolMeta] =
    val terms = query.toLowerCase.split("(?U)\\W+").filter(_.length >= 3).toList
    if terms.isEmpty then Nil
    else
      all.filter { meta =>
        val haystack = (meta.definition.name :: meta.definition.description :: meta.keywords).mkString(" ").toLowerCase
        terms.exists(haystack.contains)
      }
