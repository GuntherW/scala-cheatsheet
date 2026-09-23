package agents

import kyo.*

/** Kleine Hilfsfunktion, um vom handgerollten `web_search`-Pfad (`WebSearchClient`, siehe dort) als JSON zurückgegebenen Text robust zu decodieren.
  *
  * Anders als bei den übrigen Agenten (siehe `Agents.runStructured`), die `AI.gen[T]` nutzen und damit schemakonformes JSON auf API-Ebene erzwingen, läuft `WebSearchClient.research` außerhalb von
  * `LLM`/`AI.gen` (siehe dort für die Begründung) - das Modell liefert daher nur "bestmögliches" JSON per System-Prompt-Instruktion, manchmal in Markdown-Codefences (```json ... ``` bzw. ```JSON ... ```),
  * obwohl der System-Prompt reines JSON verlangt. Diese Funktion entfernt ein führendes Codefence (case-insensitiver, optionaler Sprach-Tag) sowie ein abschließendes Codefence vor dem Decodieren und
  * liefert bei einem regulären Decode-Fehler (`Result.Failure`, z. B. Modell hat sich nicht ans Schema gehalten) `Left` statt zu werfen, damit der Aufrufer (analog zur bestehenden Nachsicht in
  * `Agents.run`) auf `partial_failure` zurückfallen kann statt hart zu scheitern. Ein `Result.Panic` (echter, unerwarteter Defekt statt eines "das Modell hat sich nicht ans Schema gehalten") wird
  * dagegen - konsistent mit der projektweiten Konvention in `Agents.scala`/`AgentFactResearcher.scala`/`AgentPlanner.scala` - erneut geworfen statt als harmloser `partial_failure` getarnt.
  */
object JsonExtraction:

  private val leadingCodefence = "(?is)^```[a-z]*\\s*".r

  def parseLenient[T: Schema](raw: String): Either[String, T] =
    val cleaned = leadingCodefence
      .replaceFirstIn(raw.trim, "")
      .stripSuffix("```")
      .trim
    Json.decode[T](cleaned) match
      case Result.Success(value) => Right(value)
      case Result.Failure(error) => Left(s"${error.getClass.getSimpleName}: ${error.getMessage}")
      case Result.Panic(error)   => throw error
