package agents

import io.circe.{parser, Decoder}

/** Kleine Hilfsfunktion, um vom LLM als JSON zurückgegebenen Text robust zu parsen (siehe `AgentFactResearcher`/`AgentRiskAnalyst`, Abschnitt "Upstream Agent Optimisation" im README).
  *
  * Modelle liefern JSON manchmal in Markdown-Codefences (```json ... ``` oder ```JSON ... ```) statt als reine JSON-Antwort, obwohl der System-Prompt das explizit verlangt - diese Funktion entfernt
  * ein führendes Codefence (case-insensitiver, optionaler Sprach-Tag) sowie ein abschließendes Codefence vor dem Parsen. Liefert im Fehlerfall `Left` statt zu werfen, damit der Aufrufer (analog zur
  * bestehenden Nachsicht in `Agent.run`, siehe `Agent.scala`) auf Rohtext zurückfallen kann statt hart zu scheitern.
  */
object JsonExtraction:

  private val leadingCodefence = "(?is)^```[a-z]*\\s*".r

  def parseLenient[T: Decoder](raw: String): Either[String, T] =
    val cleaned = leadingCodefence
      .replaceFirstIn(raw.trim, "")
      .stripSuffix("```")
      .trim
    parser.decode[T](cleaned).left.map(_.getMessage)
